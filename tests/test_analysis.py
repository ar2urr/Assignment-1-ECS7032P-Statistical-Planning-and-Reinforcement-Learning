import importlib.util
import unittest
from pathlib import Path
import numpy as np

path = Path(__file__).resolve().parents[1] / 'scripts/analyse-study.py'
spec = importlib.util.spec_from_file_location('analysis', path)
analysis = importlib.util.module_from_spec(spec)
spec.loader.exec_module(analysis)

class AnalysisTest(unittest.TestCase):
    def test_stratification_preserves_fixed_seat_means(self):
        samples = analysis.stratified_bootstrap([0, 0, 1, 1], [0, 0, 1, 1], np.random.default_rng(5))
        np.testing.assert_array_equal(samples, np.full(5000, .5))
        self.assertEqual((.5, .5), analysis.interval(samples))

    def test_resampling_is_repeatable_and_within_observed_bounds(self):
        values, seats = [0, .5, 1, 0, 1, .5], [0, 0, 1, 1, 2, 2]
        a = analysis.stratified_bootstrap(values, seats, np.random.default_rng(5))
        b = analysis.stratified_bootstrap(values, seats, np.random.default_rng(5))
        np.testing.assert_array_equal(a, b)
        self.assertTrue(np.all((a >= 0) & (a <= 1)))
        lo, hi = analysis.interval(a)
        self.assertLess(lo, .5)
        self.assertGreater(hi, .5)

    def test_validator_rejects_incomplete_cell(self):
        with self.assertRaisesRegex(ValueError, 'Incomplete cell'):
            analysis.validate_cell([], 3, 504, 'baseline', 'random', 'iterations', set())

if __name__ == '__main__':
    unittest.main()
