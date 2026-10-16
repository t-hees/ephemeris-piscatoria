import { useNavigate, useParams } from "react-router-dom";
import { NotifiableContainer, type NotifiableContentContext } from "../components/NotifiableContainer";
import ItemAutocomplete from "../components/ItemAutocomplete";
import { useCallback, useEffect, useState } from "react";
import { Loading } from "../components/Loading";
import { fetchApi } from "../util/fetchApi";
import type { SimpleFish, SimpleCatchDto, SpecialCatchDto, SpecialCatchWithIdDto, NewSpecialCatch } from "../components/api/FishCatch";
import { MAX_IMAGE_BYTES, SpecialCatchList } from "../components/api/FishCatch";
import { environmentList, weatherList, type TripDetailDto, type TripDto, type TripFields, type environmentType, type weatherType } from "../components/api/Trip";
import { fishLabel } from "../util/fishLabel";
import { useTranslation } from "../i18n/LanguageContext";
import { useAuth } from "../util/AuthContext";

const FORM_ID = "trip-form";

const EMPTY_TRIP: TripFields = {
  location: "",
  environment: null,
  time: "",
  hours: null,
  temperature: null,
  waterLevel: null,
  weather: [],
  notes: null,
};

/**
 * Creates a trip (/trips/new) or edits an existing one (/trips/:id/edit) along with its catches
 */
export default function TripForm() {
  return (
    <NotifiableContainer MainContent={TripEditor} />
  )
}

/**
 * The multipart body creating a special catch, see SpecialCatchController in the backend
 */
function toFormData(newCatch: NewSpecialCatch): FormData {
  const specialCatch: SpecialCatchDto = {
    fishId: newCatch.fishId,
    size: newCatch.size,
    weight: newCatch.weight,
    notes: newCatch.notes,
    name: newCatch.name,
  };
  const form = new FormData();
  form.append("catch", new Blob([JSON.stringify(specialCatch)], { type: "application/json" }));
  if (newCatch.image) form.append("image", newCatch.image);
  return form;
}

function TripEditor({ setError }: NotifiableContentContext) {
  const navigate = useNavigate();
  const idParam = useParams<{ id: string }>().id;
  // Set once a new trip is created, so saving again after a failed upload doesn't create it twice
  const [tripId, setTripId] = useState<number | null>(idParam !== undefined ? Number(idParam) : null);
  const [trip, setTrip] = useState<TripFields>(EMPTY_TRIP);
  const [simpleCatches, setSimpleCatches] = useState<SimpleCatchDto[]>([]);
  const [specialCatches, setSpecialCatches] = useState<NewSpecialCatch[]>([]);
  const [oldSpecialCatches, setOldSpecialCatches] = useState<SpecialCatchWithIdDto[]>([]);
  const [removableSpecialCatchIds, setRemovableSpecialCatchIds] = useState<number[]>([]);
  const [loading, setLoading] = useState<boolean>(idParam !== undefined);
  const { t } = useTranslation();

  const isValidId = idParam === undefined || Number.isInteger(Number(idParam));

  // Replaces all local edits with the trip as currently saved
  const showSavedTrip = useCallback(async (response: Response) => {
    const saved: TripDetailDto = await response.json();
    setTrip({
      location: saved.location,
      environment: saved.environment,
      time: saved.time,
      hours: saved.hours,
      temperature: saved.temperature,
      waterLevel: saved.waterLevel,
      weather: saved.weather,
      notes: saved.notes,
    });
    setSimpleCatches(saved.simpleCatches);
    setOldSpecialCatches(saved.specialCatches);
    setSpecialCatches([]);
    setRemovableSpecialCatchIds([]);
  }, []);

  useEffect(() => {
    if (idParam !== undefined && isValidId) {
      fetchApi(`trips/${idParam}`, "GET", showSavedTrip, setError, setLoading)
    }
  }, [idParam, isValidId, showSavedTrip, setError]);

  if (!isValidId) return (<h1>{t.trip.noTripId}</h1>);
  if (loading) return <Loading />

  /*
  Saves the fields and simple catches first, then removes and uploads the special catches one by one.
  New catches that couldn't be saved stay in the form, so nothing entered is lost.
  */
  const submitTrip = async () => {
    let failed = false;
    const handleError = (error: string | null) => {
      failed = true;
      setError(error);
    };
    setLoading(true);
    setError(null);

    const data: TripDto = { ...trip, simpleCatches };
    let savedId = tripId;
    if (savedId === null) {
      await fetchApi("trips", "POST", async (response) => {
        savedId = (await response.json()).id;
        setTripId(savedId);
      }, handleError, () => {}, data);
    } else {
      await fetchApi(`trips/${savedId}`, "PUT", () => {}, handleError, () => {}, data);
    }
    if (failed || savedId === null) {
      setLoading(false);
      return;
    }

    for (const catchId of removableSpecialCatchIds) {
      if (!failed) {
        await fetchApi(`trips/${savedId}/special-catches/${catchId}`, "DELETE", () => {}, handleError, () => {});
      }
    }

    const unsavedCatches: NewSpecialCatch[] = [];
    for (const newCatch of specialCatches) {
      if (!failed) {
        await fetchApi(`trips/${savedId}/special-catches`, "POST", () => {}, handleError, () => {},
          toFormData(newCatch));
      }
      if (failed) {
        unsavedCatches.push(newCatch);
      } else if (newCatch.previewUrl) {
        URL.revokeObjectURL(newCatch.previewUrl);
      }
    }

    if (!failed) {
      navigate("/");
      return;
    }
    // Shows what got saved, the error stays visible
    await fetchApi(`trips/${savedId}`, "GET", showSavedTrip, setError, setLoading);
    setSpecialCatches(unsavedCatches);
  }

  return (
    <>
      <h2>{tripId === null ? t.trip.newTrip : t.trip.editTrip}</h2>
      <form id={FORM_ID} onSubmit={(e) => {
        e.preventDefault();
        submitTrip();
      }}>
        <TripFieldsEditor trip={trip} setTrip={setTrip} />
      </form>
      <hr />
      <SimpleCatchEditor simpleCatches={simpleCatches} setSimpleCatches={setSimpleCatches} setError={setError} />
      <SpecialCatchEditor
        oldSpecialCatches={oldSpecialCatches.filter(fish => !removableSpecialCatchIds.includes(fish.catchId))}
        removeOldSpecialCatch={(catchId) => setRemovableSpecialCatchIds([...removableSpecialCatchIds, catchId])}
        specialCatches={specialCatches}
        setSpecialCatches={setSpecialCatches}
        setError={setError}
      />
      {/* Outside of the form, so pressing enter in a fish search doesn't save the trip */}
      <button className="form-submit-button" type="submit" form={FORM_ID}>
        {t.trip.saveTrip}
      </button>
    </>
  )
}

type TripFieldsEditorType = {
  trip: TripFields,
  setTrip: React.Dispatch<React.SetStateAction<TripFields>>,
}
function TripFieldsEditor({ trip, setTrip }: TripFieldsEditorType) {
  const { t } = useTranslation();

  const textInput = (labelText: string, field: "location" | "time" | "notes",
    inputType: "text" | "datetime-local", isRequired?: boolean) => (
      <>
        <label className="form-label">{labelText}: </label>
        <input
          type={inputType}
          value={trip[field] ?? ""}
          onChange={(e) => setTrip({...trip, [field]: e.target.value})}
          required={isRequired ? true : false}
        />
      </>
    )

  const numberInput = (labelText: string, field: "hours" | "temperature" | "waterLevel") => (
      <>
        <label className="form-label">{labelText}: </label>
        <input
          type="number"
          value={trip[field] ?? ""}
          onChange={(e) => setTrip({...trip, [field]: e.target.value === "" ? null : e.target.valueAsNumber})}
        />
      </>
    )

  const toggleWeather = (element: weatherType) => {
    setTrip({...trip, weather: trip.weather.includes(element)
      ? trip.weather.filter((el) => el !== element)
      : [...trip.weather, element]});
  }

  return (
    <>
      {textInput(t.trip.dateTime, "time", "datetime-local", true)}
      {textInput(t.trip.location, "location", "text", true)}
      <label className="form-label">{t.trip.environment}: </label>
      <select
        value={trip.environment ?? ""}
        onChange={(e) => setTrip({...trip, environment: (e.target.value || null) as environmentType | null})}
      >
        <option value="">----</option>
        {environmentList.map((element) => (
          <option key={element} value={element}>
            {t.trip.environmentElement[element]}
          </option>
        ))}
      </select>
      {numberInput(t.trip.durationInHours, "hours")}
      {numberInput(t.trip.temperature, "temperature")}
      {numberInput(t.trip.waterLevel, "waterLevel")}
      <fieldset className="chip-fieldset">
        <legend className="form-label">{t.trip.weather}: </legend>
        <div className="chip-group">
          {weatherList.map((element) => {
            const checked = trip.weather.includes(element);
            return (
              <label key={element} className={checked ? "chip chip-selected" : "chip"}>
                <input type="checkbox" checked={checked} onChange={() => toggleWeather(element)} />
                {t.trip.weatherElement[element]}
              </label>
            );
          })}
        </div>
      </fieldset>
      {textInput(t.trip.notes, "notes", "text")}
    </>
  )
}

type SimpleCatchEditorType = {
  simpleCatches: SimpleCatchDto[],
  setSimpleCatches: React.Dispatch<React.SetStateAction<SimpleCatchDto[]>>,
  setError: React.Dispatch<string | null>,
}
function SimpleCatchEditor({ simpleCatches, setSimpleCatches, setError }: SimpleCatchEditorType) {
  const { t } = useTranslation();

  const updateSimpleCatches = (id: number, upFunc: (dto: SimpleCatchDto) => SimpleCatchDto) => {
    const updatedValues = simpleCatches.map(sCatch => (sCatch.fishId === id)
      ? upFunc(sCatch)
      : sCatch);
    setSimpleCatches(updatedValues);
  }

  return (
    <div>
      <h2>{t.catches.simpleCatches}</h2>
      <ItemAutocomplete<SimpleFish>
        url="fish?name="
        onSelect={(fish: SimpleFish) => (!simpleCatches.some(scatch => scatch.fishId === fish.id))
          && setSimpleCatches([...simpleCatches, {
            name: fish.commonName, scientificName: fish.scientificName, fishId: fish.id, amount: 1}])}
        displayFunc={(fish: SimpleFish) => `${fish.commonName} (${fish.scientificName})`}
        setError={setError}
      />
      <ul className="entry-list">
        {simpleCatches.map((fish) => (
          <li className="entry-list-item quantity-row" key={fish.fishId}>
            <span>{fishLabel(fish.name, fish.scientificName)}</span>
            <button type="button" onClick={() => {
              if (fish.amount < 2) {
                setSimpleCatches(simpleCatches.filter(sCatch => sCatch.fishId != fish.fishId))
              } else {
                updateSimpleCatches(fish.fishId, (dto) => {return {...dto, amount: dto.amount - 1}})
              }
            }}>
              -
            </button>
            <span>{fish.amount}</span>
            <button type="button" onClick={() => updateSimpleCatches(fish.fishId, (dto) => {return {...dto, amount: dto.amount + 1}})}>
              +
            </button>
          </li>
        ))}
      </ul>
      <hr />
    </div>
  )
}

type SpecialCatchEditorType = {
  oldSpecialCatches: SpecialCatchWithIdDto[],
  removeOldSpecialCatch: (catchId: number) => void,
  specialCatches: NewSpecialCatch[],
  setSpecialCatches: React.Dispatch<React.SetStateAction<NewSpecialCatch[]>>,
  setError: React.Dispatch<string | null>,
}
function SpecialCatchEditor({ oldSpecialCatches, removeOldSpecialCatch, specialCatches, setSpecialCatches,
  setError }: SpecialCatchEditorType) {
  const { t } = useTranslation();
  // Uploaded images aren't moderated, so the public demo only lets admins upload them
  const { isAdmin } = useAuth();

  // By position, as there can be several detailed catches of the same fish
  const updateSpecialCatches = (index: number, upFunc: (dto: NewSpecialCatch) => NewSpecialCatch) => {
    const updatedValues = specialCatches.map((sCatch, idx) => (idx === index)
      ? upFunc(sCatch)
      : sCatch);
    setSpecialCatches(updatedValues);
  }

  const selectImage = (index: number, image: File | undefined) => {
    if (image && image.size > MAX_IMAGE_BYTES) {
      setError(t.catches.imageTooLarge(MAX_IMAGE_BYTES / 1024 / 1024));
      return;
    }
    updateSpecialCatches(index, (dto) => {
      if (dto.previewUrl) URL.revokeObjectURL(dto.previewUrl);
      return {...dto, image: image ?? null, previewUrl: image ? URL.createObjectURL(image) : null};
    });
  }

  const deleteSpecialCatchButton = (fish: SpecialCatchWithIdDto) => {
    return (
      <button type="button" className="danger" onClick={() => removeOldSpecialCatch(fish.catchId)}>
        {t.catches.deleteFish}
      </button>
    )
  }

  return (
    <div>
      <h2>{t.catches.detailedCatches}</h2>
      <SpecialCatchList specialCatches={oldSpecialCatches} action={deleteSpecialCatchButton} />
      <ItemAutocomplete<SimpleFish>
        url="fish?name="
        onSelect={(fish: SimpleFish) => setSpecialCatches([...specialCatches, {
            name: fish.commonName,
            scientificName: fish.scientificName,
            fishId: fish.id,
            image: null,
            previewUrl: null,
            size: null,
            weight: null,
            notes: null,
        }])}
        displayFunc={(fish: SimpleFish) => `${fish.commonName} (${fish.scientificName})`}
        setError={setError}
      />
      <ul className="entry-list">
        {specialCatches.map((fish, index) => (
          // New catches are only ever appended, so their position is a stable key
          <li className="entry-list-item" key={index}>
            <h3>{fishLabel(fish.name, fish.scientificName)}</h3>
            <label className="form-label">{t.catches.photo}</label>
            {fish.previewUrl &&
              <img src={fish.previewUrl} alt={t.catches.photoOf(fishLabel(fish.name, fish.scientificName))}/>
            }
            {isAdmin
              ? <input
                  type="file"
                  accept="image/jpeg,image/png"
                  onChange={(e) => selectImage(index, e.target.files?.[0])}
                />
              : <p>{t.catches.photoUploadDisabled}</p>
            }
            <label className="form-label">{t.catches.size}</label>
            <input
              type="number"
              value={fish.size ? fish.size : ""}
              onChange={(e) => updateSpecialCatches(index, (dto) => {return {...dto, size:e.target.valueAsNumber}})}
            />
            <label className="form-label">{t.catches.weight}</label>
            <input
              type="number"
              value={fish.weight ? fish.weight : ""}
              onChange={(e) => updateSpecialCatches(index, (dto) => {return {...dto, weight:e.target.valueAsNumber}})}
            />
            <label className="form-label">{t.catches.notes}</label>
            <input
              type="text"
              value={fish.notes ? fish.notes : ""}
              onChange={(e) => updateSpecialCatches(index, (dto) => {return {...dto, notes:e.target.value}})}
            />
          </li>
        ))}
      </ul>
      <hr />
    </div>
  )
}
