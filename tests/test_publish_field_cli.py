from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from breathing_willow_cli.breathing_willow import main as cli_main
import types

fake_md = types.SimpleNamespace(markdown=lambda x: x)


def test_publish_field_publish(monkeypatch):
    monkeypatch.setitem(sys.modules, 'markdown', fake_md)
    monkeypatch.setitem(sys.modules, 'googleapiclient.discovery', types.SimpleNamespace(build=lambda *a, **k: None))
    monkeypatch.setitem(sys.modules, 'googleapiclient.http', types.SimpleNamespace(MediaIoBaseUpload=lambda *a, **k: None))
    monkeypatch.setitem(sys.modules, 'oauth2client.file', types.SimpleNamespace(Storage=lambda *a, **k: None))
    monkeypatch.setitem(sys.modules, 'oauth2client.client', types.SimpleNamespace(flow_from_clientsecrets=lambda *a, **k: None))
    monkeypatch.setitem(sys.modules, 'oauth2client.tools', types.SimpleNamespace(run_flow=lambda *a, **k: None))
    from breathing_willow import field_publish
    calls = {}
    def fake_publish(fp):
        calls['fp'] = fp
    monkeypatch.setattr(field_publish, 'publish', fake_publish)
    cli_main(['publish-field', '-f', 'x.md', '--publish'])
    assert calls['fp'] == 'x.md'


def test_publish_field_update(monkeypatch):
    monkeypatch.setitem(sys.modules, 'markdown', fake_md)
    monkeypatch.setitem(sys.modules, 'googleapiclient.discovery', types.SimpleNamespace(build=lambda *a, **k: None))
    monkeypatch.setitem(sys.modules, 'googleapiclient.http', types.SimpleNamespace(MediaIoBaseUpload=lambda *a, **k: None))
    monkeypatch.setitem(sys.modules, 'oauth2client.file', types.SimpleNamespace(Storage=lambda *a, **k: None))
    monkeypatch.setitem(sys.modules, 'oauth2client.client', types.SimpleNamespace(flow_from_clientsecrets=lambda *a, **k: None))
    monkeypatch.setitem(sys.modules, 'oauth2client.tools', types.SimpleNamespace(run_flow=lambda *a, **k: None))
    from breathing_willow import field_publish
    calls = {}
    def fake_update(url, fp):
        calls['url'] = url
        calls['fp'] = fp
    monkeypatch.setattr(field_publish, 'update', fake_update)
    cli_main(['publish-field', '-f', 'a.md', '--update', '-u', 'http://doc'])
    assert calls['fp'] == 'a.md'
    assert calls['url'] == 'http://doc'
