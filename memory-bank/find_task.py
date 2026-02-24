import json, pathlib, argparse

p=pathlib.Path(r'c:\Users\User\AllMine\prog\prog_java\mts\Courses\memory-bank\tasks.json')
d=json.loads(p.read_text(encoding='utf-8'))
changed=False

parser = argparse.ArgumentParser(description='Обновление статуса задачи в JSON-файле')
parser.add_argument('task_name', help='ID задачи (например, TASK-019A)')

args = parser.parse_args()

for t in d.get('tasks',[]):
    if t.get('id')==args.task_name:
        print(t); changed=True; break
if not changed: raise SystemExit(args.task_name + ' not found')
