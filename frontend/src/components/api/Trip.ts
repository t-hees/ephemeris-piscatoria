export const environmentList = ["RIVER", "LAKE", "OCEAN"] as const;
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

export type TripDto = {
  location: string,
  environment: environmentType,
  time: number,
  hours: number,
  temperature: number,
  waterLevel: number,
  weather: weatherType[],
  notes: string,
}
