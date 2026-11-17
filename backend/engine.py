from dataclasses import dataclass

@dataclass(frozen=True)
class Candle:
    timestamp: str
    high: float
    low: float
    close: float

def rma(values, period):
    out = [None] * len(values)
    if len(values) < period: return out
    out[period-1] = sum(values[:period]) / period
    for i in range(period, len(values)):
        out[i] = (out[i-1] * (period-1) + values[i]) / period
    return out

def calculate(candles, atr_period=10, multiplier=3.0, adx_period=14, min_adx=25):
    n = len(candles)
    if n < max(atr_period, adx_period * 2) + 3: return []
    tr, pdm, mdm = [], [], []
    for i, c in enumerate(candles):
        prev = candles[i-1] if i else c
        tr.append(max(c.high-c.low, abs(c.high-prev.close), abs(c.low-prev.close)))
        up, down = c.high-prev.high, prev.low-c.low
        pdm.append(up if i and up > down and up > 0 else 0.0)
        mdm.append(down if i and down > up and down > 0 else 0.0)
    atr = rma(tr, atr_period)
    smooth_tr, smooth_plus, smooth_minus = rma(tr[1:], adx_period), rma(pdm[1:], adx_period), rma(mdm[1:], adx_period)
    dx = [None] * n
    for i in range(adx_period, n):
        j = i-1
        if smooth_tr[j] and smooth_tr[j] > 0:
            pdi = 100*smooth_plus[j]/smooth_tr[j]
            mdi = 100*smooth_minus[j]/smooth_tr[j]
            dx[i] = 100*abs(pdi-mdi)/(pdi+mdi) if pdi+mdi else 0.0
    adx = [None]*n
    first = 2*adx_period-1
    adx[first] = sum(dx[adx_period:first+1])/adx_period
    for i in range(first+1,n): adx[i] = (adx[i-1]*(adx_period-1)+dx[i])/adx_period
    upper, lower, trend = [None]*n, [None]*n, [None]*n
    for i in range(atr_period-1,n):
        c = candles[i]
        mid = (c.high+c.low)/2
        bu, bl = mid+multiplier*atr[i], mid-multiplier*atr[i]
        if i == atr_period-1:
            upper[i], lower[i], trend[i] = bu, bl, 1
            continue
        prev = candles[i-1]
        upper[i] = bu if bu < upper[i-1] or prev.close > upper[i-1] else upper[i-1]
        lower[i] = bl if bl > lower[i-1] or prev.close < lower[i-1] else lower[i-1]
        if trend[i-1] == -1:
            trend[i] = 1 if c.close > upper[i-1] else -1
        else:
            trend[i] = -1 if c.close < lower[i-1] else 1
    result=[]
    for i in range(first+1,n):
        signal = None
        if trend[i] != trend[i-1] and adx[i] is not None and adx[i] > min_adx:
            signal = 'BUY' if trend[i] == 1 else 'SELL'
        result.append({'timestamp':candles[i].timestamp,'close':candles[i].close,'adx':round(adx[i],2),'trend':trend[i],'signal':signal})
    return result
