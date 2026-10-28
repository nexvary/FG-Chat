#!/usr/bin/env python3
"""Scan exact resolved Android runtime Maven versions against OSV, fail closed.
No source or user data is uploaded, only public Maven package/version identifiers.
Native advisories require separate upstream review (ADR-0004).
"""
import json,pathlib,urllib.request,datetime
root=pathlib.Path(__file__).resolve().parents[1]
manifest=root/'android-app/build/reports/dependencies/maven.tsv'
packages=[line.split('\t') for line in manifest.read_text().splitlines() if line]
assert packages, 'No resolved dependencies were exported'
findings=[]
pending=[(name,version,None) for name,version in packages]
while pending:
    queries=[]
    batch=pending[:100];pending=pending[100:]
    for name,version,page in batch:
        query={'package':{'ecosystem':'Maven','name':name},'version':version}
        if page:query['page_token']=page
        queries.append(query)
    req=urllib.request.Request('https://api.osv.dev/v1/querybatch',data=json.dumps({'queries':queries}).encode(),headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(req,timeout=60) as response:results=json.load(response)['results']
    assert len(results)==len(batch), 'OSV response incomplete'
    for (name,version,_),result in zip(batch,results):
        for item in result.get('vulns',[]):findings.append({'package':name,'version':version,'id':item['id']})
        if result.get('next_page_token'):pending.append((name,version,result['next_page_token']))
out=root/'build/reports/dependency-scan.json';out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps({'time':datetime.datetime.now(datetime.timezone.utc).isoformat(),'source':'https://api.osv.dev/v1/querybatch','scope':'resolved Android runtime Maven artifacts; native/build/test dependencies not scanned','packages':len(packages),'findings':findings},indent=2)+'\n')
print(f'Scanned {len(packages)} Android runtime Maven artifacts; {len(findings)} vulnerability records')
for finding in findings:print(finding)
raise SystemExit(1 if findings else 0)
