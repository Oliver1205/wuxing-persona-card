interface PersonaLabelSource {
  personaLabel?: string;
  starToneName?: string;
  primaryElement?: string;
  secondaryElement?: string;
}

export function normalizePersonaLabel(source: PersonaLabelSource | null | undefined) {
  const starTone = source?.starToneName?.trim() ?? '';
  if (isValidStarToneName(starTone)) {
    return starTone;
  }
  const legacyLabel = source?.personaLabel?.trim() ?? '';
  if (isValidStarToneName(legacyLabel)) {
    return legacyLabel;
  }
  return fallbackPersonaLabel();
}

function isValidStarToneName(value: string) {
  const chars = Array.from(value);
  return (
    chars.length === 4 &&
    !value.includes('的') &&
    chars.every((char) => /^[\u4e00-\u9fff]$/.test(char))
  );
}

function fallbackPersonaLabel() {
  return '星曜成象';
}
