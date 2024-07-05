import os
import glob

tmp_files = glob.glob("**/*.tmp", recursive=True)
for file in tmp_files:
    os.remove(file)

cpp_files = glob.glob("**/*.cpp", recursive=True)

for file in cpp_files:
    base = os.path.splitext(file)[0]
    os.rename(file, base + ".c")