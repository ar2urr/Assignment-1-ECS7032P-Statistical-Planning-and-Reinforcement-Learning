#!/usr/bin/env python3
"""Compile an exported package in a fresh TAG source tree and exercise its real entry points."""
import argparse
import hashlib
import importlib.util
import json
import os
import re
import subprocess
import zipfile
from pathlib import Path
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(command, log, cwd=None):
    with log.open('a') as stream:
        result = subprocess.run(command, cwd=cwd, stdout=stream, stderr=subprocess.STDOUT)
    if result.returncode:
        raise RuntimeError('Command failed; see ' + str(log))


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('archive', type=Path)
    p.add_argument('tag', type=Path, help='Local Git checkout supplying a clean committed TAG tree')
    p.add_argument('--output', type=Path, required=True, help='New directory for fresh sources, logs and validation')
    p.add_argument('--revision', help='Explicit course compatibility target; defaults to archive TAG_COMMIT')
    args = p.parse_args()
    archive, tag, out = args.archive.resolve(), args.tag.resolve(), args.output.resolve()
    with zipfile.ZipFile(archive) as z:
        names = z.namelist()
        if not names or len(names) != len(set(names)):
            p.error('Empty archive or duplicate members')
        package = names[0].split('/')[0]
        if package != 'sushigoagent' and not re.fullmatch(r'group[A-Z]+', package):
            p.error('Unexpected package name')
        expected = {package+'/'+name for name in ('BasicMCTSParams.java','BasicMCTSPlayer.java','BasicTreeNode.java',
                    'SearchBudget.java','SushiGoHeuristic.java','SubmissionPlayer.java','competition.json','README.md','LICENSE-TAG.txt','TAG_COMMIT')}
        if set(names) != expected or any(i.file_size > 1_000_000 for i in z.infolist()):
            p.error('Archive must contain only the documented package files')
        revision = args.revision or z.read(package+'/TAG_COMMIT').decode().strip()
        commit = subprocess.check_output(['git','-C',str(tag),'rev-parse',revision+'^{commit}'],text=True).strip()
        if not re.fullmatch(r'[0-9a-f]{40}', commit): p.error('Invalid resolved revision')
        if out.exists(): p.error('Output exists; use a fresh directory')
        out.mkdir(parents=True)
        fresh = out/'TabletopGames'; fresh.mkdir()
        tar = out/'framework.tar'
        with tar.open('wb') as stream:
            subprocess.run(['git','-C',str(tag),'archive','--format=tar',commit],stdout=stream,check=True)
        subprocess.run(['tar','-xf',str(tar),'-C',str(fresh)],check=True); tar.unlink()
        baseline = {str(f.relative_to(fresh)):digest(f) for f in fresh.rglob('*') if f.is_file()}
        for name in names:
            destination = fresh/'src/main/java'/name
            if destination.exists(): p.error('Export would replace an existing framework file')
            destination.parent.mkdir(parents=True,exist_ok=True)
            destination.write_bytes(z.read(name))
    cp_file = out/'dependencies.txt'
    command = [os.environ.get('MVN','mvn'),'-B','-q','-f',str(fresh/'pom.xml'),'-DskipTests','compile',
               'dependency:build-classpath','-DincludeScope=test','-Dmdep.outputFile='+str(cp_file)]
    if os.environ.get('MAVEN_REPO'): command += ['-Dmaven.repo.local='+os.environ['MAVEN_REPO']]
    run(command,out/'build.log')
    classpath = str(fresh/'target/classes')+os.pathsep+cp_file.read_text().strip()
    probe_classes = out/'probe-classes'; probe_classes.mkdir()
    probe = ROOT/'tests/coursework/validation/SubmissionProbe.java'
    run(['javac','--release','21','-proc:none','-cp',classpath,'-d',str(probe_classes),str(probe)],out/'build.log')
    run(['java','-Xms128m','-Xmx512m','-Djava.awt.headless=true','-cp',str(probe_classes)+os.pathsep+classpath,
         'coursework.validation.SubmissionProbe',str(fresh/'src/main/java'/package/'competition.json'),package,str(out)],out/'probe.log',fresh)
    changed = [rel for rel,sha in baseline.items() if not (fresh/rel).is_file() or digest(fresh/rel)!=sha]
    if changed: raise RuntimeError('Framework source changed: '+repr(changed))
    result = json.loads((out/'probe.json').read_text())
    result.update({'archive_sha256':digest(archive),'package':package,'development_package':package=='sushigoagent',
                   'tag_revision':commit,'framework_source_files_unchanged':len(baseline),'probe_sha256':digest(probe),
                   'java_version':subprocess.run(['java','-version'],capture_output=True,text=True).stderr.strip(),
                   'finished_utc':datetime.now(timezone.utc).isoformat(),
                   'classpath_policy':'Fresh TAG classes and dependencies plus external probe; no development project classes'})
    (out/'validation.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))

if __name__=='__main__': main()
