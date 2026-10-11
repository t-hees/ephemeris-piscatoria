// name is the common name the user picked for the fish, if any
export function fishLabel(name: string|null, scientificName: string|null): string {
  if (!name) return scientificName ?? "";
  return scientificName ? `${name} (${scientificName})` : name;
}
