import unittest
from evaluate_runs import compare


class PerformanceComparatorTest(unittest.TestCase):
    def sample(self, p95=40, p99=60, chunks=20):
        return {
            'environment': {
                'minecraft': '26.3', 'java': '25', 'hardware': 'test host',
                'mkm_sha': 'a1b2c3', 'world_seed': '42', 'scenario': 'combat40',
                'player_count': 1, 'entity_count': 40, 'render_distance': 10,
                'simulation_distance': 6, 'duration_seconds': 600,
            },
            'metrics': {'tick_p95_ms': p95, 'tick_p99_ms': p99,
                        'chunks_per_second': chunks},
        }

    def test_safe_speedup(self):
        self.assertTrue(compare(self.sample(), self.sample(33, 57, 25))['meets_provisional_performance_gate'])

    def test_p99_regression_rejected(self):
        self.assertFalse(compare(self.sample(), self.sample(30, 70, 25))['meets_provisional_performance_gate'])

    def test_not_comparable_worlds(self):
        a, b = self.sample(), self.sample()
        b['environment']['world_seed'] = 'other'
        with self.assertRaises(ValueError): compare(a, b)

    def test_missing_metrics(self):
        a, b = self.sample(), self.sample()
        del b['metrics']['tick_p95_ms']
        with self.assertRaises(ValueError): compare(a, b)


if __name__ == '__main__':
    unittest.main()
