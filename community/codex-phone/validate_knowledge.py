"""Validate local reference integrity only; never execute phone or model tools."""
from pathlib import Path
import hashlib
import json
import tomllib

root = Path(__file__).resolve().parent
doc = json.loads((root / 'knowledge.json').read_text())
assert doc['schema_version'] == '1.0' and doc['revision'] == 2
assert doc['purpose'] == 'retrieval_and_learning_reference'
assert doc['security_status'] == 'not_fully_passed'
assert doc['software_modified'] is False
sources = {s['id']: s for s in doc['sources']}
claims = {c['id']: c for c in doc['claims']}
assert len(sources) == len(doc['sources']) and len(claims) == len(doc['claims'])
for source in sources.values():
    path = (root / source['path']).resolve()
    assert path.is_relative_to(root)
    raw = path.read_bytes()
    assert hashlib.sha256(raw).hexdigest() == source['sha256'], source['id']
    assert len(raw.decode().splitlines()) == source['line_count']
    assert '/' + doc['source_snapshot_commit'] + '/' in source['immutable_raw_url']
for item in doc['claims'] + doc['tools']:
    if 'evidence_type' in item:
        assert item['evidence_type'] in doc['evidence_types']
    assert item['scope_limit'] and item['evidence']
    for ref in item['evidence']:
        source = sources[ref['source_id']]
        assert 1 <= ref['line_start'] <= ref['line_end'] <= source['line_count']
config = tomllib.loads((root / 'materials/templates/config.toml').read_text())
assert {t['id'] for t in doc['tools']} == set(config['mcp_servers']['stepneo-phone']['enabled_tools'])
assert len(doc['tools']) == 9
for item in doc['findings']:
    assert item['precondition'] and item['recommendation']
    assert item['exploit_reproduced_on_android'] is False
    assert all(i in claims for i in item['related_claims'])
for item in doc['learning_path'] + doc['answer_checks']:
    assert all(i in claims for i in item['claim_ids'])
checks = json.loads((root / 'checks.json').read_text())
assert len(checks['checks']) == 28 and all(c['passed'] for c in checks['checks'])
manifest = json.loads((root / 'manifest.json').read_text())
for item in manifest['files']:
    assert hashlib.sha256((root / 'materials' / item['file']).read_bytes()).hexdigest() == item['sha256']
print(json.dumps({'reference_integrity': 'passed', 'sources': len(sources),
                  'claims': len(claims), 'tools': len(doc['tools']),
                  'historical_checks': 28, 'new_phone_tests': 0}))
