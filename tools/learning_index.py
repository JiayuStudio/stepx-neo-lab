#!/usr/bin/env python3
"""Rebuild additive learning metadata in the existing catalog and Markdown index."""
import argparse
import hashlib
import json
import re
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]
DATE = datetime.now(timezone.utc).date().isoformat()
REPO = 'https://github.com/JiayuStudio/stepx-neo-lab'
RAW = 'https://raw.githubusercontent.com/JiayuStudio/stepx-neo-lab/main/'
ROOTS = ('courses', 'guides', 'official', 'library', 'community')
ARCHIVE = 'courses/archive/official-step-studio-2026-10-20-early/'
POLICY = 'maintenance/learning-policy.md'


def build():
    catalog = json.loads((ROOT / 'catalog.json').read_text())
    previous = {e['id']: e for e in catalog.get('learning', {}).get('documents', [])}
    course = json.loads((ROOT / 'courses/official-step-studio/data.json').read_text())
    official = {}
    for item in course['entries'] + course['components']:
        if item.get('path'):
            official['courses/official-step-studio/' + item['path']] = item['official_url']
    documents = []
    for path in sorted(p for root in ROOTS for p in (ROOT / root).rglob('*.md')):
        rel = path.relative_to(ROOT).as_posix()
        content = path.read_text()
        title = re.search(r'^#\s+(.+)', content, re.M)
        archived = rel.startswith(ARCHIVE)
        source_urls = sorted(set(re.findall(r'https://studio\.stepx\.com/[^\s)\]>"`]*', content)))
        if rel in official:
            source_urls = sorted(set(source_urls + [official[rel]]))
        replacement = rel.replace(ARCHIVE, 'courses/official-step-studio/') if archived else None
        if replacement and not (ROOT / replacement).exists():
            replacement = 'courses/official-step-studio/README.md'
        identifier = 'doc:' + rel
        source_urls = sorted(set(source_urls + previous.get(identifier, {}).get('official_sources', [])))
        entry = {
            'id': identifier, 'title': title.group(1) if title else path.stem,
            'path': rel,
            'kind': 'supporting_reference' if '/source/' in rel or '/materials/' in rel else 'learning_document',
            'audience': ['human', 'agent'],
            'human_url': REPO + '/blob/main/' + quote(rel),
            'raw_url': RAW + quote(rel),
            'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            'content_status': 'archived' if archived else 'indexed',
            'availability_status': 'review_required',
            'inventory_checked': DATE,
            'last_official_verified': None,
            'official_sources': source_urls,
            'evidence_scope': 'inventory_only; preserve_verification_limits_in_source',
            'source_notes': '官方链接仅供核查；缺少链接不代表无来源，详见原文。',
            'verification_record': 'maintenance/2026-10-23-review.md',
            'replacement_path': replacement,
            'retirement_reason': '早期课程已归档；不是平台功能下架。' if archived else None,
        }
        if rel.startswith('official/'):
            entry['evidence_scope'] = 'historical_group_pdf; original_message_not_independently_verified'
            entry['source_notes'] = '来源及页码见 official/step-studio-knowledge/data.json；2026-10-20 是收录日期。'
        elif rel.startswith('library/'):
            entry['evidence_scope'] = 'source_report_not_new_test'
        elif rel in official:
            entry['last_official_verified'] = course['checked']
            entry['evidence_scope'] = 'historical_official_page_review; execution_not_verified_this_round'
        # Curated factual annotations survive regeneration. Hashes, titles and links do not.
        for key in ('content_status', 'availability_status', 'inventory_checked', 'last_official_verified',
                    'evidence_scope', 'source_notes', 'verification_record', 'replacement_path', 'retirement_reason'):
            if identifier in previous:
                entry[key] = previous[identifier][key]
        documents.append(entry)
    external = []
    for name in ('手机安装教程', '常见问题', '实测记录'):
        rel = 'docs/' + name + '.md'
        external.append({
            'id': 'external:gbox:' + rel, 'title': 'GBox ' + name,
            'human_url': 'https://github.com/JiayuStudio/stepx-neo-gbox-guide/blob/main/' + quote(rel),
            'raw_url': 'https://raw.githubusercontent.com/JiayuStudio/stepx-neo-gbox-guide/main/' + quote(rel),
            'audience': ['human', 'agent'], 'content_status': 'external_reference',
            'availability_status': 'review_required', 'inventory_checked': DATE,
            'last_official_verified': None, 'official_sources': ['https://www.gboxlab.com/'],
            'evidence_scope': 'existing_repository_link; external_content_not_reverified',
            'replacement_path': None, 'retirement_reason': None,
        })
    old_external = {e['id']: e for e in catalog.get('learning', {}).get('external_documents', [])}
    for entry in external:
        if entry['id'] in old_external:
            for key in ('inventory_checked', 'availability_status', 'last_official_verified', 'evidence_scope', 'replacement_path', 'retirement_reason'):
                entry[key] = old_external[entry['id']][key]
    catalog['learning'] = {
        'schema_version': 1, 'policy': POLICY,
        'human_start': 'sections/start-here.md', 'agent_start': 'llms.txt',
        'markdown_index': 'sections/learning-index.md',
        'scope': {'roots': list(ROOTS), 'pattern': '**/*.md', 'includes_archives_and_supporting_references': True,
                  'external_scope': 'three GBox pages already linked by sections/guides.md; not entire external repository'},
        'counts': {'local_documents': len(documents), 'external_references': len(external),
                   'by_root': dict(sorted(Counter(e['path'].split('/')[0] for e in documents).items())),
                   'archived': sum(e['content_status'] == 'archived' for e in documents),
                   'supporting_references': sum(e['kind'] == 'supporting_reference' for e in documents)},
        'documents': documents, 'external_documents': external,
    }
    lines = ['# 逐页学习索引', '', '[研究所首页](../README.md) · [人类入门](start-here.md) · [Agent 入口](../llms.txt) · [维护口径](../'+POLICY+')', '',
             '此页由 `python3 tools/learning_index.py` 生成。逐页来源、核验日期、状态、哈希及替代路径见 [catalog.json 的 learning 字段](../catalog.json)。', '',
             f'本地 {len(documents)} 页，外部引用 {len(external)} 页；归档 {catalog["learning"]["counts"]["archived"]} 页。收录覆盖不等于功能已复验。', '',
             '| 页面 | 原始 Markdown | 内容状态 |', '| --- | --- | --- |']
    for e in documents:
        lines.append(f'| [{e["title"].replace("|", "／")}](../{quote(e["path"])}) | [Raw]({e["raw_url"]}) | {e["content_status"]} |')
    lines += ['', '## 外部教程引用', '']
    for e in external:
        lines.append(f'- [{e["title"]}]({e["human_url"]}) · [Raw]({e["raw_url"]}) · 尚未复核外部正文')
    return catalog, '\n'.join(lines) + '\n'


def validate(catalog):
    learning = catalog['learning']
    docs = learning['documents']
    assert len({e['id'] for e in docs}) == len(docs), 'duplicate IDs'
    for e in docs:
        assert e['availability_status'] in {'review_required', 'documented', 'access_failed', 'deprecated', 'retired'}
        assert e['content_status'] in {'indexed', 'archived'}
        if e['replacement_path']:
            assert (ROOT / e['replacement_path']).is_file(), e['replacement_path']
        if e['availability_status'] in {'deprecated', 'retired'}:
            assert e['retirement_reason'] and e['verification_record'], e['id']
        assert (ROOT / e['verification_record']).is_file(), e['id']
        if e['last_official_verified']:
            assert e['official_sources'], e['id']
    assert {e['path'] for e in docs} == {p.relative_to(ROOT).as_posix() for r in ROOTS for p in (ROOT/r).rglob('*.md')}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    catalog, index = build()
    validate(catalog)
    outputs = {'catalog.json': json.dumps(catalog, ensure_ascii=False, indent=2) + '\n', 'sections/learning-index.md': index}
    for name, text in outputs.items():
        if args.check:
            assert (ROOT/name).read_text() == text, f'{name} drift; run python3 tools/learning_index.py'
        else:
            (ROOT/name).write_text(text)
    print(json.dumps(catalog['learning']['counts'], ensure_ascii=False))


if __name__ == '__main__':
    main()
