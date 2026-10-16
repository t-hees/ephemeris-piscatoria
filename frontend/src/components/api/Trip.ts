import type { SimpleCatchDto, SpecialCatchWithIdDto } from "./FishCatch";

export const environmentList =["RIVER", "LAKE", "OCEAN"] as const;
export const weatherList = [
        "CLEAR_SKY",
        "PARTLY_CLOUDY",
        "CLOUDY",
        "OVERCAST",
        "FOGGY",
        "HUMID",
        "THUNDERSTORM",
        "LIGHT_RAIN",
        "HEAVY_RAIN",
        "LIGHT_WIND",
        "STRONG_WIND",
        "STORM",
        "FREEZING",
        "SNOW",
        "DUSK",
        "DAWN",
] as const;

export type environmentType = typeof environmentList[number];
export type weatherType = typeof weatherList[number];

export type TripFields = {
  location: string,
  environment: environmentType | null,
  // Local date time like 2026-04-05T06:30
  time: string,
  hours: number | null,
  temperature: number | null,
  waterLevel: number | null,
  weather: weatherType[],
  notes: string | null,
}

// Special catches are created and deleted one at a time, see SpecialCatchController in the backend
export type TripDto = TripFields & {
  simpleCatches: SimpleCatchDto[],
}

export type TripDetailDto = TripFields & {
  id: number,
  simpleCatches: SimpleCatchDto[],
  specialCatches: SpecialCatchWithIdDto[],
}
