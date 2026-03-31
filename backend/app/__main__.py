from __future__ import annotations

import uvicorn

from .settings import settings


def main() -> None:
    uvicorn.run("app.main:app", host="127.0.0.1", port=settings.port)


if __name__ == "__main__":
    main()

