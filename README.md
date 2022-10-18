# PPS-Project-ZIO-Demo

An interactive websocket-backed chat application running on `zio`, `zio-http` and `scalajs-dom`. It's a demo develop for the course "Paradigmi di Programmazione e Sviluppo", 2020-2021.

The demo is part of the report available at the following [link](https://github.com/amarfoglia/PPS-Project-ZIO).

## Project structure

The project is an `sbt` multi-project build.

- `common`: includes the domain objects shared between frontend and backend;
- `backend`: includes the business logic exploited by a `zio-http` server;
- `frontend`: define the UI connecting to the server.

## How to Run
1. compile the frontend `sbt ~frontend/fastLinkJS`.
2. start the server `sbt ~backend/run`.
3. open `frontend/index.html` with a browser.