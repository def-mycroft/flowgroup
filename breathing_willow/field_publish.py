
import os
from os.path import expanduser, join
import io
import markdown
from googleapiclient.discovery import build
from googleapiclient.http import MediaIoBaseUpload
from oauth2client.file import Storage
from oauth2client.client import flow_from_clientsecrets
from oauth2client.tools import run_flow

SCOPES = ['https://www.googleapis.com/auth/drive.file']
CLIENT_SECRET_FILE = join(expanduser('~'), 'credentials', 
                          "client-secret-traced-beetle-29168f97.json")
CREDENTIALS_PATH = os.path.expanduser('~/.config/field-publish-credentials.json')


def _get_drive_service():
    store = Storage(CREDENTIALS_PATH)
    creds = store.get()
    if not creds or creds.invalid:
        flow = flow_from_clientsecrets(CLIENT_SECRET_FILE, SCOPES)
        creds = run_flow(flow, store)
    return build('drive', 'v3', credentials=creds)

def publish(filepath):
    if not filepath.endswith('.md'):
        raise ValueError("Only .md files are supported")

    with open(filepath, 'r') as f:
        md_content = f.read()

    html_content = markdown.markdown(md_content)

    media = MediaIoBaseUpload(
        io.BytesIO(html_content.encode('utf-8')),
        mimetype='text/html',
        resumable=True
    )

    service = _get_drive_service()
    file_metadata = {
        'name': os.path.basename(filepath).replace('.md', ''),
        'mimeType': 'application/vnd.google-apps.document'
    }

    uploaded = service.files().create(
        body=file_metadata,
        media_body=media,
        fields='id'
    ).execute()

    file_id = uploaded.get('id')

    service.permissions().create(
        fileId=file_id,
        body={'type': 'anyone', 'role': 'reader'}
    ).execute()

    link = f"https://docs.google.com/document/d/{file_id}/edit"
    print(link)
    return link


def _doc_id_from_url(url: str) -> str:
    import re

    m = re.search(r"/d/([a-zA-Z0-9_-]+)", url)
    if not m:
        raise ValueError("unable to parse document id from url")
    return m.group(1)


def update(url: str, filepath: str) -> str:
    """Update an existing Google Doc with contents of ``filepath``."""
    if not filepath.endswith('.md'):
        raise ValueError("Only .md files are supported")

    with open(filepath, 'r') as f:
        md_content = f.read()

    html_content = markdown.markdown(md_content)

    media = MediaIoBaseUpload(
        io.BytesIO(html_content.encode('utf-8')),
        mimetype='text/html',
        resumable=True,
    )

    file_id = _doc_id_from_url(url)
    service = _get_drive_service()
    service.files().update(fileId=file_id, media_body=media).execute()

    link = f"https://docs.google.com/document/d/{file_id}/edit"
    print(link)
    return link

