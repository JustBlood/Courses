import json, pathlib, argparse; p=pathlib.Path(r'c:\Users\User\AllMine\prog\prog_java\mts\Courses\memory-bank\tasks.json'); d=json.loads(p.read_text(encoding='utf-8')); changed=False

parser = argparse.ArgumentParser(description='Обновление статуса задачи в JSON-файле')
parser.add_argument('task_name', help='ID задачи (например, TASK-019A)')
parser.add_argument('status',
                    choices=['pending', 'done', 'obsolete'],
                    help='Новый статус задачи')
parser.add_argument('--file', '-f',
                    default='tasks.json',
                    help='Путь к JSON-файлу с задачами (по умолчанию: tasks.json)')

args = parser.parse_args()

for t in d.get('tasks',[]):
    if t.get('id')==args.task_name:
        t['status']=args.status; changed=True; break
if not changed: raise SystemExit('TASK-019A not found')
p.write_text(json.dumps(d, ensure_ascii=False, indent=2)+ '\n', encoding='utf-8')
