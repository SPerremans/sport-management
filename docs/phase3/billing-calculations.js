// Billing calculation reference. Prepared source, not a deployed Mendix microflow.
// UTC dates; interval ends are exclusive. Monetary inputs and outputs use integer cents.
const DAY = 86400000;
const MODES = ["ACTIVE_AT_CENSUS", "UNIQUE_DURING_YEAR", "PRORATA_ACTIVE_DAYS"];
function instant(value) {
  const n = typeof value === "number" ? value : Date.parse(value);
  if (!Number.isFinite(n)) throw new Error("Invalid date");
  return n;
}
function cents(n) {
  if (!Number.isSafeInteger(n) || n < 0) throw new Error("Invalid fee in cents");
  return n;
}
function roundRatio(numerator, denominator) {
  const n = BigInt(numerator), d = BigInt(denominator);
  if (d <= 0n || n < 0n) throw new Error("Invalid ratio");
  const result = (n * 2n + d) / (d * 2n);
  if (result > BigInt(Number.MAX_SAFE_INTEGER)) throw new Error("Amount exceeds safe currency range");
  return Number(result);
}
function quoteAnnual({year, mode, censusAt, now, registrations}) {
  if (!MODES.includes(mode) || !Number.isInteger(year)) throw new Error("Unsupported billing method");
  const from = Date.UTC(year, 0, 1), until = Date.UTC(year + 1, 0, 1);
  const asOf = instant(now), census = mode === MODES[0] ? instant(censusAt) : null;
  if (census !== null && (census < from || census >= until || census > asOf)) throw new Error("Census must be in the billing year and not in the future");
  const final = mode === MODES[0] ? census <= asOf : asOf >= until;
  const grouped = new Map();
  for (const r of registrations) {
    if (!r.personId) throw new Error("Member profile is required");
    // A projection of a pending request is not an activated membership.
    if (r.joinedAt == null) continue;
    const fee = cents(r.annualFeeCents); // Missing rates fail the run rather than underbill.
    const start = Math.max(from, instant(r.joinedAt), instant(r.seasonStartsOn), instant(r.affiliationStartsOn));
    const end = Math.min(until, r.endedAt == null ? Infinity : instant(r.endedAt), instant(r.seasonEndsExclusive), r.affiliationEndsExclusive == null ? Infinity : instant(r.affiliationEndsExclusive));
    if (end <= start || start > asOf) continue;
    if (!grouped.has(r.personId)) grouped.set(r.personId, []);
    grouped.get(r.personId).push({start, end, fee});
  }
  const lines = [], yearDays = (until - from) / DAY;
  for (const [personId, periods] of grouped) {
    if (mode === MODES[0]) {
      const eligible = periods.filter(p => p.start <= census && census < p.end);
      if (eligible.length) lines.push({personId, activeDays: null, billableUnits: 1, amountCents: Math.max(...eligible.map(p => p.fee))});
    } else if (mode === MODES[1]) {
      const eligible = periods.filter(p => p.start < Math.min(until, asOf) && p.end > from);
      if (eligible.length) lines.push({personId, activeDays: null, billableUnits: 1, amountCents: Math.max(...eligible.map(p => p.fee))});
    } else {
      let activeDays = 0, weightedCents = 0n;
      // A calendar day with any active interval counts once; use the highest overlapping rate.
      for (let d = from; d < Math.min(until, asOf); d += DAY) {
        const eligible = periods.filter(p => p.start < Math.min(d + DAY, asOf) && p.end > d);
        if (!eligible.length) continue;
        activeDays += 1;
        weightedCents += BigInt(Math.max(...eligible.map(p => p.fee)));
      }
      if (activeDays) lines.push({personId, activeDays, billableUnits: activeDays / yearDays, amountCents: roundRatio(weightedCents, yearDays)});
    }
  }
  const totalCents = lines.reduce((sum, line) => sum + BigInt(line.amountCents), 0n);
  if (totalCents > BigInt(Number.MAX_SAFE_INTEGER)) throw new Error("Invoice exceeds safe currency range");
  return {year, mode, censusAt: census, final, yearDays, memberCount: lines.length, billableUnits: lines.reduce((s,l) => s+l.billableUnits,0), amountCents: Number(totalCents), lines};
}
function instalments({totalCents, requested = 1, approved = 1, allowRequested = false, maximum = 12}) {
  cents(totalCents);
  if (![requested, approved, maximum].every(Number.isInteger) || requested < 1 || approved < 1 || maximum < 1 || maximum > 12) throw new Error("Invalid instalment request");
  if (approved > requested || approved > maximum || (approved > 1 && !allowRequested)) throw new Error("Approval exceeds request or club policy");
  const count = Math.min(approved, Math.max(1,totalCents));
  const base = Math.floor(totalCents / count);
  return Array.from({length:count},(_,i) => i===count-1 ? totalCents-base*(count-1) : base);
}
return {DAY, MODES, quoteAnnual, instalments, roundRatio};

