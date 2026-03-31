from __future__ import annotations

import uvicorn

from app.main import app as fastapi_app
from app.settings import settings


def main() -> None:
    uvicorn.run(fastapi_app, host="127.0.0.1", port=settings.port)


if __name__ == "__main__":
    main()

