import yt_dlp
import json


def download(quickjs_bin: str, video_id: str) -> str:
    # Keep all audio-stream extraction in one call, without the unnecessary
    # four-client metadata lookup or megabytes of formatted JSON.
    opts = {
        "format": "bestaudio",
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
        "socket_timeout": 8,
        "retries": 1,
        "extractor_retries": 1,
        "fragment_retries": 1,
        "js_runtimes": {"quickjs": {"path": quickjs_bin}},
    }
    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(video_id, download=False)

    if not isinstance(info, dict) or not info.get("url"):
        raise ValueError("yt-dlp returned no playable audio URL")

    selected_id = info.get("format_id")
    selected = next(
        (fmt for fmt in info.get("formats", [])
         if fmt.get("format_id") == selected_id),
        {},
    )
    ext = info.get("ext") or selected.get("ext")
    mime_type = {
        "m4a": "audio/mp4",
        "mp4": "audio/mp4",
        "webm": "audio/webm",
        "mp3": "audio/mpeg",
        "ogg": "audio/ogg",
        "opus": "audio/ogg",
    }.get(ext)

    # The player only uses these fields. Returning the complete yt-dlp result
    # is slow and consumes considerable memory on Android.
    return json.dumps({
        "id": info.get("id"),
        "format_id": selected_id,
        "url": info["url"],
        "filesize": info.get("filesize") or selected.get("filesize"),
        "filesize_approx": info.get("filesize_approx") or selected.get("filesize_approx"),
        "duration": info.get("duration"),
        "mime_type": mime_type,
        "formats": [{
            "format_id": selected_id,
            "abr": info.get("abr") or selected.get("abr"),
        }] if selected_id else [],
    }, separators=(",", ":"))


def upgrade(package_name):
    try:
        import ensurepip

        ensurepip.bootstrap()
    except Exception as e:
        print(f"Error running ensurepip: ${e}")

    try:
        import pip
        from pip._internal import main as pip_main

        result = pip_main(["install", "--upgrade", package_name])
        if result != 0:
            raise RuntimeError(f"pip exited with status {result}")
        print(f"Successfully upgraded {package_name}")
    except Exception as e:
        print(f"Error upgrading package {package_name}: {e}")
        raise
