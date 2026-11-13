import unittest
from engine import Candle,calculate
class EngineTests(unittest.TestCase):
 def test_flat_prices_do_not_trigger(self):
  bars=[Candle(str(i),101,99,100) for i in range(100)]
  self.assertTrue(all(row['signal'] is None for row in calculate(bars)))
 def test_short_history(self):
  self.assertEqual(calculate([Candle('0',2,1,1.5)]),[])
 def test_valid_output(self):
  bars=[Candle(str(i),101+i*.2,99+i*.2,100+i*.2) for i in range(100)]
  self.assertTrue(calculate(bars))
if __name__=='__main__': unittest.main()
