import { useState } from "react";
import { NotifiableContainer, type NotifiableContentContext } from "../components/NotifiableContainer";
import { fetchApi } from "../util/fetchApi";
import { useNavigate } from "react-router-dom";
import { Loading } from "../components/Loading";
import { environmentList, weatherList, type environmentType, type TripDto, type weatherType } from "../components/api/Trip";
import { useTranslation } from "../i18n/LanguageContext";

export default function CreateTrip() {
  return (
    <NotifiableContainer MainContent={Trip}/>
  );
}

function Trip({ setError }: NotifiableContentContext) {
  const navigate = useNavigate();
  const [loading, setLoading] = useState<boolean>(false);
  const [trip, setTripDto] = useState<TripDto>({} as TripDto);
  const { t } = useTranslation();

  const handleResponse = async () => {
    navigate("/");
  }

  const handleError = (err: string|null) => {
    setLoading(false);
    setError(err);
  }

  const tripFromInput = (labelText: string, dtoElement: keyof TripDto,
    labelType: "text"|"number"|"datetime-local", isRequired?: boolean) => (
      <>
        <label className="form-label">{labelText}: </label>
        <input
          type={labelType}
          value={trip[dtoElement]}
          onChange={(e) => setTripDto({...trip, [dtoElement]: e.target.value})}
          required={isRequired ? true : false}
        />
      </>
    )


  const tripEnvironmentSelection = (labelText: string, dtoElement: keyof TripDto,
    environmentList: readonly environmentType[]) => (
      <>
        <label className="form-label">{labelText}: </label>
        <select
          value={trip[dtoElement] ?? ""}
          onChange={(e) => setTripDto({...trip, [dtoElement]: e.target.value || undefined})}
        >
          <option value="">----</option>
          {environmentList.map((element, idx) => (
            <option key={idx} value={element}>
              {t.trip.environmentElement[element]}
            </option>
          ))}
        </select>
      </>
    )

  const toggleWeather = (element: weatherType) => {
    const selected = trip.weather ?? [];
    setTripDto({...trip, weather: selected.includes(element)
      ? selected.filter((el) => el !== element)
      : [...selected, element]});
  }

  const tripWeatherSelection = (labelText: string, weatherList: readonly weatherType[]) => (
      <fieldset className="chip-fieldset">
        <legend className="form-label">{labelText}: </legend>
        <div className="chip-group">
          {weatherList.map((element) => {
            const checked = trip.weather?.includes(element) ?? false;
            return (
              <label key={element} className={checked ? "chip chip-selected" : "chip"}>
                <input type="checkbox" checked={checked} onChange={() => toggleWeather(element)} />
                {t.trip.weatherElement[element]}
              </label>
            );
          })}
        </div>
      </fieldset>
    )

  return (
    <>
      <h2> {t.trip.newTrip} </h2>
      {loading && <Loading />}
      <form onSubmit={(e) => {
        e.preventDefault();
        fetchApi("trips", "POST", handleResponse, handleError, setLoading, trip)
      }}>
        {tripFromInput(t.trip.dateTime, "time", "datetime-local", true)}
        {tripFromInput(t.trip.location, "location", "text", true)}
        {tripEnvironmentSelection(t.trip.environment, "environment", environmentList)}
        {tripFromInput(t.trip.durationInHours, "hours", "number")}
        {tripFromInput(t.trip.temperature, "temperature", "number")}
        {tripFromInput(t.trip.waterLevel, "waterLevel", "number")}
        {tripWeatherSelection(t.trip.weather, weatherList)}
        {tripFromInput(t.trip.notes, "notes", "text")}
        <div>
          <button className="form-submit-button" type="submit">{t.trip.createTrip}</button>
        </div>
      </form>
    </>
  );
}
