#!/usr/bin/env python3
"""Client for the owner-paired Codex task service on this phone."""
import argparse
import json
import re
import sys
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path

BASE = 'http://127.0.0.1:19094'
CREDENTIALS = Path.home() / '.local/state/stepneo-phone-codex/client.json'
HTTP = urllib.request.build_opener(urllib.request.ProxyHandler({}))


class BridgeError(Exception):
    def __init__(self, payload):
        self.payload = payload


def request(path, token, body=None, timeout=3):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode('utf-8')
    req = urllib.request.Request(BASE + path, data=data, headers={
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json',
    })
    try:
        with HTTP.open(req, timeout=timeout) as response:
            return json.loads(response.read(100000))
    except urllib.error.HTTPError as error:
        try:
            payload = json.loads(error.read(4096))
        except (ValueError, OSError):
            payload = {'error': 'http_error'}
        payload['http_status'] = error.code
        raise BridgeError(payload) from None
    except (OSError, ValueError):
        raise BridgeError({'error': 'bridge_unreachable'}) from None


def wait_for(task_id, token, seconds):
    deadline = time.monotonic() + seconds
    result = None
    while result is None or time.monotonic() < deadline:
        remaining = deadline - time.monotonic()
        timeout = min(3, max(0.1, remaining)) if seconds else 3
        result = request('/v1/tasks/' + task_id, token, timeout=timeout)
        if result.get('status') in ('completed', 'failed'):
            return result
        remaining = deadline - time.monotonic()
        if remaining > 0:
            time.sleep(min(1, remaining))
    result['next'] = 'Query status for this same task_id; do not submit a replacement.'
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='action', required=True)
    run = sub.add_parser('run')
    run.add_argument('--request-id', required=True)
    run.add_argument('--prompt', required=True)
    run.add_argument('--wait', type=int, default=4, choices=range(0, 7), metavar='0..6')
    status = sub.add_parser('status')
    status.add_argument('task_id')
    status.add_argument('--wait', type=int, default=4, choices=range(0, 7), metavar='0..6')
    sub.add_parser('doctor')
    args = parser.parse_args()
    try:
        credentials = json.loads(CREDENTIALS.read_text(encoding='utf-8'))
        token = credentials['token']
        if not re.fullmatch('[a-f0-9]{64}', token):
            raise ValueError('invalid token')
    except (OSError, ValueError, KeyError, TypeError):
        print(json.dumps({'error': 'client_not_paired'}))
        return 2

    task_id = args.task_id if args.action == 'status' else (
        args.request_id if args.action == 'run' else 'doctor-' + uuid.uuid4().hex)
    if not re.fullmatch('[A-Za-z0-9][A-Za-z0-9_-]{0,63}', task_id):
        print(json.dumps({'error': 'invalid_task_id'}))
        return 2
    try:
        if args.action != 'status':
            body = {'request_id': task_id, 'kind': 'status' if args.action == 'doctor' else 'task'}
            if args.action == 'run':
                body['prompt'] = args.prompt
            try:
                request('/v1/tasks', token, body)
            except BridgeError as error:
                if error.payload.get('error') == 'bridge_unreachable':
                    error.payload.update(task_id=task_id, submission='unknown',
                        next='Query this same task_id. Do not create a new task after a timeout.')
                raise
        result = wait_for(task_id, token, getattr(args, 'wait', 4))
        print(json.dumps(result, ensure_ascii=False))
        return 1 if result.get('status') == 'failed' else 0
    except BridgeError as error:
        error.payload.setdefault('task_id', task_id)
        print(json.dumps(error.payload, ensure_ascii=False))
        return 2


if __name__ == '__main__':
    sys.exit(main())
