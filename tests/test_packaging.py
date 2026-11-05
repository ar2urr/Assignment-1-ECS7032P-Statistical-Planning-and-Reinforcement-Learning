import hashlib
import importlib.util
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('packaging_script', ROOT / 'scripts/package-submission.py')
packaging = importlib.util.module_from_spec(spec)
spec.loader.exec_module(packaging)

class PackagingTest(unittest.TestCase):
    def test_export_is_reproducible_and_contains_only_standalone_runtime_files(self):
        before = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT / 'sushigoagent').iterdir() if p.is_file()}
        with tempfile.TemporaryDirectory() as tmp:
            a, b = Path(tmp)/'first.zip', Path(tmp)/'second.zip'
            packaging.build(ROOT, 'groupAG', a)
            packaging.build(ROOT, 'groupAG', b)
            self.assertEqual(a.read_bytes(), b.read_bytes())
            with zipfile.ZipFile(a) as z:
                expected = {'groupAG/' + name for name in (*packaging.SOURCES, 'competition.json', 'README.md', 'LICENSE-TAG.txt', 'TAG_COMMIT')}
                self.assertEqual(set(z.namelist()), expected)
                self.assertEqual(json.loads(z.read('groupAG/competition.json'))['class'], 'groupAG.SubmissionPlayer')
                for name in packaging.SOURCES:
                    self.assertTrue(z.read('groupAG/' + name).startswith(b'package groupAG;'))
                    self.assertNotIn(b'sushigoagent', z.read('groupAG/' + name))
        after = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT / 'sushigoagent').iterdir() if p.is_file()}
        self.assertEqual(before, after)

    def test_invalid_or_unassigned_submission_package_is_rejected(self):
        for package in ('', '../groupB', 'groupB.nested', 'sushigoagent', 'group', 'groupB/evil'):
            with self.subTest(package=package), self.assertRaises(ValueError):
                packaging.package_files(ROOT, package)

    def test_existing_archive_is_not_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)/'existing.zip'; path.write_bytes(b'existing data')
            with self.assertRaises(FileExistsError):
                packaging.build(ROOT, 'groupAG', path)
            self.assertEqual(path.read_bytes(), b'existing data')

    def test_draft_is_explicit_and_keeps_development_namespace(self):
        data = packaging.package_files(ROOT, 'sushigoagent', draft=True)
        self.assertIn(b'not a final submission ZIP', data['README.md'])
        self.assertEqual(json.loads(data['competition.json'])['class'], 'sushigoagent.SubmissionPlayer')

if __name__ == '__main__':
    unittest.main()
