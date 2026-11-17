"""Gold Alert backend. Run with: uvicorn server:app --host 0.0.0.0 --port 8000"""
import os
from datetime import datetime, timezone
import httpx
from fastapi import FastAPI, HTTPException
from engine import Candle, calculate

app = FastAPI(title='Gold Alert API')
API_KEY = os.getenv('TWELVE_DATA_API_KEY','')

@app.get('/health')
def health(): return {'ok':True}

@app.get('/signals')
async def signals(
    interval: str='5min',
    atr_period: int=10,
    multiplier: float=3.0,
    adx_period: int=14,
    min_adx: float=25.0
):
    if interval not in {'1min','5min','15min','30min','1h','4h'}:
        raise HTTPException(400,'Unsupported interval')
    if not API_KEY: raise HTTPException(503,'Set TWELVE_DATA_API_KEY on server')
    async with httpx.AsyncClient(timeout=20) as client:
        response = await client.get('https://api.twelvedata.com/time_series',params={'symbol':'XAU/USD','interval':interval,'outputsize':160,'apikey':API_KEY,'timezone':'UTC'})
        response.raise_for_status()
        data = response.json()
    if data.get('status')=='error' or 'values' not in data:
        raise HTTPException(502, data.get('message','Provider did not return candles'))
    raw = sorted(data['values'],key=lambda x:x['datetime'])
    # Only finalized candles; current candle is excluded based on UTC interval boundaries.
    seconds = {'1min':60,'5min':300,'15min':900,'30min':1800,'1h':3600,'4h':14400}[interval]
    now = datetime.now(timezone.utc).timestamp()
    candles=[]
    for x in raw:
        t = datetime.fromisoformat(x['datetime']).replace(tzinfo=timezone.utc).timestamp()
        if t+seconds <= now:
            candles.append(Candle(x['datetime'],float(x['high']),float(x['low']),float(x['close']),float(x['open'])))
    results = calculate(candles, atr_period=atr_period, multiplier=multiplier, adx_period=adx_period, min_adx=min_adx)
    if not results: raise HTTPException(502,'Not enough finalized candles')
    return {'symbol':'XAUUSD','interval':interval,'latest':results[-1],'indicators':results[-100:],'candles':[{'time':c.timestamp,'open':c.open,'high':c.high,'low':c.low,'close':c.close} for c in candles[-100:]]}

@app.get("/chart")
async def chart(
    interval: str = "5min",
    atr_period: int = 10,
    multiplier: float = 3.0,
    adx_period: int = 14,
    min_adx: float = 25.0
):
    from fastapi.encoders import jsonable_encoder

    result = await signals(
        interval=interval,
        atr_period=atr_period,
        multiplier=multiplier,
        adx_period=adx_period,
        min_adx=min_adx
    )

    return {
        "symbol": "XAUUSD",
        "interval": interval,
        "latest": jsonable_encoder(result["latest"]),
        "status": "connected",
        "candles": result["candles"],
        "indicators": result["indicators"]
    }
