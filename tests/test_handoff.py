import copy
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'tools'))
from check_handoff import validate


def sample():
    return {'schema_version': 1, 'tasks': [
        {'id': 'T00', 'title': 'First', 'depends_on': [], 'requirements': ['REQ-01'],
         'deliverables': ['docs/a.md'], 'steps': ['Inspect'], 'checks': ['Run test'],
         'manual_check': 'Inspect result', 'human_gate': None, 'status': 'pending', 'evidence': []},
        {'id': 'T01', 'title': 'Second', 'depends_on': ['T00'], 'requirements': ['REQ-02'],
         'deliverables': ['docs/b.md'], 'steps': ['Build'], 'checks': ['Run test'],
         'manual_check': 'Owner accepts', 'human_gate': 'G0', 'status': 'pending', 'evidence': []},
    ]}


class HandoffTests(unittest.TestCase):
    def check_error(self, payload, text):
        self.assertTrue(any(text in e for e in validate({'REQ-01', 'REQ-02'}, payload)))

    def test_valid_pending_plan(self):
        self.assertEqual([], validate({'REQ-01', 'REQ-02'}, sample()))

    def test_rejects_duplicate_task(self):
        p = sample(); p['tasks'].append(copy.deepcopy(p['tasks'][0]))
        self.check_error(p, 'duplicate')

    def test_rejects_missing_dependency(self):
        p = sample(); p['tasks'][1]['depends_on'] = ['T99']
        self.check_error(p, 'unknown dependency')

    def test_rejects_dependency_cycle(self):
        p = sample(); p['tasks'][0]['depends_on'] = ['T01']
        self.check_error(p, 'cycle')

    def test_rejects_uncovered_requirement(self):
        p = sample(); p['tasks'][1]['requirements'] = ['REQ-01']
        self.check_error(p, 'uncovered')

    def test_rejects_invented_requirement(self):
        p = sample(); p['tasks'][1]['requirements'].append('BOGUS-99')
        self.check_error(p, 'unknown requirement')

    def test_rejects_verified_without_evidence(self):
        p = sample(); p['tasks'][0]['status'] = 'verified'
        self.check_error(p, 'evidence')

    def test_rejects_accepted_gate_without_owner_approval(self):
        p = sample(); p['tasks'][1].update(status='accepted', evidence=['actual.log'])
        self.check_error(p, 'owner approval')

    def test_rejects_progress_with_pending_dependency(self):
        p = sample(); p['tasks'][1]['status'] = 'in_progress'
        self.check_error(p, 'dependency not complete')

    def test_rejects_multiple_in_progress(self):
        p = sample()
        for t in p['tasks']: t.update(status='in_progress', depends_on=[])
        self.check_error(p, 'multiple')

    def test_rejects_absolute_delivery_path(self):
        p = sample(); p['tasks'][0]['deliverables'] = ['/tmp/output']
        self.check_error(p, 'relative')

    def test_rejects_invalid_status(self):
        p = sample(); p['tasks'][0]['status'] = 'done-ish'
        self.check_error(p, 'status')

    def test_rejects_empty_verification(self):
        p = sample(); p['tasks'][0]['checks'] = []
        self.check_error(p, 'checks')


if __name__ == '__main__':
    unittest.main()
