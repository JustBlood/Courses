import json, pathlib; p=pathlib.Path(r'c:\Users\User\AllMine\prog\prog_java\mts\Courses\memory-bank\tasks.json'); d=json.loads(p.read_text(encoding='utf-8')); changed=False

sys.argv
for t in d.get('tasks',[]):
    if t.get('id')=='TASK-019A':
        t['status']='done'; changed=True
if not changed: raise SystemExit('TASK-019A not found')
p.write_text(json.dumps(d, ensure_ascii=False, indent=2)+ '\n', encoding='utf-8')
