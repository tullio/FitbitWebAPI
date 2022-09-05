import unittest
import PersistenceDiagramTransform as pdt
import datetime

class PersistenceDiagramTransformSpec(unittest.TestCase):
    def test_index_transform(self):
        self.assertEqual(pdt.index_to_time_string(100), "0 0:1:40")
        start_date = pdt.start_date
        td_1d = datetime.timedelta(days=1)
        target_date = start_date + td_1d
        self.assertEqual(pdt.time_to_index(target_date), 60*60*24)

if __name__ == '__main__':
    unittest.main()
