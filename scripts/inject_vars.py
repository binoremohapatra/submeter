import os

files_to_inject = {
    r'd:\submeter\frontend\src\app\dashboard\plans\[id]\page.tsx': '''
const MODEL_LABEL: Record<string, string> = {
  FLAT_RATE: "Flat Rate",
  PER_UNIT: "Per Unit",
  TIERED: "Tiered",
  VOLUME: "Volume",
};
const MODEL_DESC: Record<string, string> = {
  FLAT_RATE: "A single fixed price per billing period.",
  PER_UNIT: "Price scales linearly with the quantity purchased.",
  TIERED: "Different prices apply to different ranges of quantity.",
  VOLUME: "The price of all units is determined by the total quantity.",
};
''',
    r'd:\submeter\frontend\src\app\dashboard\plans\page.tsx': '''
const MODEL_LABEL: Record<string, string> = {
  FLAT_RATE: "Flat Rate",
  PER_UNIT: "Per Unit",
  TIERED: "Tiered",
  VOLUME: "Volume",
};
const INTERVAL_LABEL: Record<string, string> = {
  MONTHLY: "mo",
  ANNUAL: "yr",
};
''',
    r'd:\submeter\frontend\src\app\dashboard\subscriptions\[id]\page.tsx': '''
const LIFECYCLE_STAGES = [
  { status: "TRIALING", label: "Trial" },
  { status: "ACTIVE", label: "Active" },
  { status: "PAST_DUE", label: "Past Due" },
  { status: "CANCELED", label: "Canceled" },
  { status: "PAUSED", label: "Paused" },
];
const STATUS_COLOR: Record<string, string> = {
  TRIALING: "var(--amber)",
  ACTIVE: "var(--green)",
  PAST_DUE: "var(--red)",
  CANCELED: "var(--neutral-500)",
  PAUSED: "var(--neutral-400)",
};
'''
}

for path, code in files_to_inject.items():
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    idx = content.find('export default function')
    if idx != -1:
        new_content = content[:idx] + code + content[idx:]
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print('Fixed', path)
