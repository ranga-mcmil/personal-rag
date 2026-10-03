Markdown · second-brain-README.md

# Personal RAG

A Spring Boot 4 backend that lets you upload your own PDFs and notes, then actually ask questions about them in plain English and get answers grounded in what you uploaded — real retrieval-augmented generation (RAG), not a toy demo. Every document is private to the user who uploaded it.

No frontend. This is a backend project meant to be exercised with `curl` or an API client, built to demonstrate a real RAG pipeline end to end: real file parsing, a deliberate chunking strategy, vector search via `pgvector`, a real local LLM for generation, and real multi-user auth protecting all of it.

## What it does

**Auth**

- `POST /auth/register`, `POST /auth/login` — real registration and login, passwords hashed with BCrypt, JWTs issued for every subsequent request.

**Ingestion**

- `POST /documents` (multipart file upload) — extracts text from a PDF or plain text file, splits it into overlapping chunks, and saves both the document and its chunks, tied to whoever uploaded it.

**Embedding**

- `POST /embed` — generates a vector embedding for every chunk that doesn't already have one (scoped to the calling user), stored in `pgvector` via plain JDBC.

**Question-answering** — *the actual point of this project*

- `POST /ask` — embeds your question, retrieves the most relevant chunks from your own documents by cosine distance, and hands them to a local LLM (via Spring AI's `ChatClient`) to generate an answer grounded in that context, with cited sources.

**Docs**

- `/swagger-ui.html` — every endpoint above, live and callable, auto-generated from the controllers via `springdoc-openapi`.

## Stack

- Spring Boot 4, Spring Web, Spring Data JPA, **Spring Security** (JWT-based, stateless)
- Postgres (via Docker) with `pgvector`
- Apache PDFBox for real PDF text extraction
- Spring AI 2.0.x with **Ollama** (`nomic-embed-text` for embeddings, `llama3.2` for chat) — both free, local, no API key
- Plain `JdbcTemplate` (not JPA) for the `embedding` column, via `pgvector-java`
- `springdoc-openapi` for live API docs
- Lombok, Spring Boot DevTools

## Project structure

```
com.example.secondbrain
├── SecondbrainApplication
├── config
│   ├── PasswordConfig          — BCryptPasswordEncoder bean
│   ├── SecurityConfig          — SecurityFilterChain, JWT filter wiring, stateless sessions
│   └── ChatClientConfig        — ChatClient bean, built from Spring AI's auto-configured Builder
├── security
│   ├── JwtService               — issues and verifies signed tokens
│   └── JwtAuthFilter            — OncePerRequestFilter; populates SecurityContextHolder per request
├── controller
│   ├── AuthController           — POST /auth/register, /auth/login
│   ├── DocumentController       — POST /documents
│   ├── EmbeddingController      — POST /embed
│   └── AskController            — POST /ask
├── dto
│   ├── RegisterRequest / LoginRequest / AuthResponse   — records
│   ├── RetrievedChunk                                  — record; chunk + source + similarity
│   ├── AskRequest / ChatAnswer                         — records
├── entity
│   ├── User              — username, passwordHash
│   ├── Document           — filename, sourceType, uploadedAt, owner (→ User)
│   └── DocumentChunk      — content, chunkIndex, embedded flag, document (→ Document); embedding column unmapped, handled via JdbcTemplate
├── repository
│   ├── UserRepository
│   ├── DocumentRepository
│   └── DocumentChunkRepository    — findPendingByOwner, two-hop JPQL join
└── service
    ├── TextExtractionService      — PDFBox for PDFs, plain read otherwise
    ├── ChunkingService            — 1000-char chunks, 200-char overlap
    ├── DocumentIngestionService   — ties extraction + chunking + persistence together, per user
    ├── ChunkEmbeddingService      — batched embedding, scoped to pending + owned chunks
    └── RagService                 — retrieval (cosine distance, owner-scoped) + grounded generation
```

## Running it

1. **Postgres with `pgvector`**:

   ```bash
   docker run --name second-brain-db \
     -e POSTGRES_DB=secondbrain \
     -e POSTGRES_USER=secondbrain \
     -e POSTGRES_PASSWORD=secondbrain \
     -p 5432:5432 \
     -d pgvector/pgvector:pg17
   ```

   Then, via `psql`:

   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ALTER TABLE document_chunk ADD COLUMN embedding vector(768);
   ```
2. **Ollama**, with both models pulled (reuses the same container across projects if you've already got one running):

   ```bash
   docker run -d --name movie-concierge-ollama -p 11434:11434 -v ollama:/root/.ollama ollama/ollama
   docker exec -it movie-concierge-ollama ollama pull nomic-embed-text
   docker exec -it movie-concierge-ollama ollama pull llama3.2
   ```
3. **A JWT secret**, exported as an environment variable:

   ```bash
   export JWT_SECRET="$(openssl rand -base64 32)"
   ```
4. Start the app, then, in order:

   ```bash
   curl -X POST "http://localhost:8080/auth/register" -H "Content-Type: application/json" -d '{"username": "you", "password": "something-real"}'
   export TOKEN="<token from the response above>"

   curl -X POST "http://localhost:8080/documents" -H "Authorization: Bearer $TOKEN" -F "file=@/path/to/your/notes.pdf"
   curl -X POST "http://localhost:8080/embed" -H "Authorization: Bearer $TOKEN"
   curl -X POST "http://localhost:8080/ask" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"question": "..."}'
   ```

## What this project was for

- **Spring Security with JWTs, end to end** — password hashing, token issuance/verification, a request filter, and a security filter chain, deliberately built for repetition's sake as much as for this project's actual need for per-user privacy.
- **Real text extraction** from PDFs via Apache PDFBox, and a genuine chunking strategy (size + overlap) with honest trade-offs explained, not just implemented.
- **The full RAG pipeline** — chunk, embed, retrieve by cosine distance, ground an LLM's answer in that retrieved context — reusing and correcting real lessons from AI Movie Concierge (task-prefixed embeddings, only embedding what's pending).
- **Ownership as a query-level guarantee, not a controller-level afterthought** — every retrieval and embedding query has the user-ownership join built directly into it.
- **`springdoc-openapi`** for API docs that stay accurate because they're generated, not hand-maintained.
- **A genuinely honest limitation, tested on purpose**: grounding instructions reduce but don't eliminate hallucination, and retrieved document content is untrusted input a malicious upload could try to use for prompt injection.

## Known gaps (left as exercises, not oversights)

- No dedupe on `/documents` — re-uploading the same file creates a second, separate `Document`.
- No minimum-similarity threshold on `/ask` — a question unrelated to anything uploaded still retrieves `TOP_K` chunks and answers from them, rather than recognizing "nothing relevant exists."
- Auth error handling is a plain `RuntimeException` (→ generic `500`), not a proper `@ExceptionHandler` returning a clean `401`.
- No `hnsw`/`ivfflat` index on `embedding` — fine at this scale, a real bottleneck at real scale.
- Naive character-count chunking, not sentence- or paragraph-aware.
- No tests — deliberately deferred project-wide until core Spring Boot fundamentals are solid across more projects.