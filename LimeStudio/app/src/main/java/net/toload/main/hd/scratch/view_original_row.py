import subprocess

cmd = "git show origin/main:LimeStudio/app/src/main/java/net/toload/main/hd/candidate/CandidateView.kt"
res = subprocess.check_output(cmd, shell=True).decode('utf-8')

# Search for CandidateRow in res
lines = res.split('\n')
start = -1
for i, line in enumerate(lines):
    if 'fun CandidateRow' in line:
        start = i
        break

if start != -1:
    print("\n".join(lines[start:start+150]))
else:
    print("CandidateRow not found")
