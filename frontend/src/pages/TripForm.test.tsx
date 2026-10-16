import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import TripForm from './TripForm';
import { jsonResponse, mockFetch, requestBodies } from '../test/mockFetch';
import { MAX_IMAGE_BYTES, type SimpleFish } from '../components/api/FishCatch';
import type { TripDetailDto } from '../components/api/Trip';
import { translations } from '../i18n/LanguageContext';
import { AuthContext, type AuthType } from '../util/AuthContext';

const t = translations.de;

const defaultAuth: AuthType = {
  name: null, isAdmin: false, isAuthenticated: () => true,
  fetchName: () => {}, setName: () => {}, authClear: () => {},
};

const aalmutter: SimpleFish = { id: 1, scientificName: "Zoarces viviparus", commonName: "Aalmutter" };
// Search results and catches are both shown with this label
const aalmutterLabel = "Aalmutter (Zoarces viviparus)";
const savedTrip: TripDetailDto = {
  id: 5,
  location: "cool lake",
  environment: "LAKE",
  time: "2026-04-05T06:30:00",
  hours: 3,
  temperature: null,
  waterLevel: null,
  weather: ["FOGGY"],
  notes: null,
  simpleCatches: [],
  specialCatches: [
    { catchId: 7, fishId: 2, imageUrl: "/api/trips/5/special-catches/7/image", size: 10, weight: 20,
      notes: "old notes", name: "Meeraal", scientificName: "Conger conger" },
  ],
};

const uploadError = { code: "UNSUPPORTED_IMAGE", message: "Only JPEG and PNG images are supported",
  timestamp: "", uri: "" };

const homeText = "home page";

function renderTripForm(path: string, createSpecialCatch = () => jsonResponse({}, 201), isAdmin = true) {
  // More specific paths first, as routes match by prefix
  const fetchMock = mockFetch({
    "POST trips/5/special-catches": createSpecialCatch,
    "DELETE trips/5/special-catches/": () => new Response(null, { status: 204 }),
    "GET trips/5": () => jsonResponse(savedTrip),
    "PUT trips/5": () => jsonResponse(savedTrip),
    "POST trips": () => jsonResponse(savedTrip, 201),
    "GET fish?name=": () => jsonResponse([aalmutter]),
  });
  render(
    <AuthContext.Provider value={{ ...defaultAuth, name: "john", isAdmin }}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/" element={<p>{homeText}</p>} />
          <Route path="/trips/new" element={<TripForm />} />
          <Route path="/trips/:id/edit" element={<TripForm />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>
  );
  return fetchMock;
}

async function addSimpleCatch(query = "aal") {
  const [simpleSearch] = await screen.findAllByPlaceholderText(t.common.searchPlaceholder);
  await userEvent.type(simpleSearch, query);
  await userEvent.click(await screen.findByText(aalmutterLabel, { selector: ".item-search-list-item" }));
}

// Queries differ, as repeating the same one within the debounce delay doesn't search again
async function addSpecialCatches(queries: string[]) {
  for (const query of queries) {
    const specialSearch = (await screen.findAllByPlaceholderText(t.common.searchPlaceholder))[1];
    await userEvent.type(specialSearch, query);
    await userEvent.click(await screen.findByText(aalmutterLabel, { selector: ".item-search-list-item" }));
  }
}

async function fillRequiredFields() {
  const [time, location] = document.querySelectorAll<HTMLInputElement>("form input");
  await userEvent.type(time, "2026-04-05T06:30");
  await userEvent.type(location, "cool lake");
}

function simpleCatchRow(): HTMLElement {
  return document.querySelector<HTMLElement>(".quantity-row")!;
}

function fileInputs(): HTMLInputElement[] {
  return Array.from(document.querySelectorAll<HTMLInputElement>("input[type=file]"));
}

async function catchPart(form: FormData): Promise<unknown> {
  return JSON.parse(await (form.get("catch") as Blob).text());
}

describe("TripForm", () => {
  beforeEach(() => {
    // Not implemented by jsdom
    URL.createObjectURL = vi.fn(() => "blob:preview");
    URL.revokeObjectURL = vi.fn();
  });

  it("creates a trip together with its catches", async () => {
    const fetchMock = renderTripForm("/trips/new");
    expect(screen.getByRole("heading", { name: t.trip.newTrip })).toBeInTheDocument();
    await fillRequiredFields();
    await userEvent.click(screen.getByLabelText(t.trip.weatherElement.FOGGY));
    await addSimpleCatch();
    await addSpecialCatches(["mutter"]);

    await userEvent.click(screen.getByText(t.trip.saveTrip));

    expect(await screen.findByText(homeText)).toBeInTheDocument();
    expect(requestBodies(fetchMock, "trips", "POST")).toEqual([{
      location: "cool lake", environment: null, time: "2026-04-05T06:30", hours: null, temperature: null,
      waterLevel: null, weather: ["FOGGY"], notes: null,
      simpleCatches: [{ name: "Aalmutter", scientificName: "Zoarces viviparus", fishId: 1, amount: 1 }],
    }]);
    // The special catch is added to the created trip
    expect(requestBodies(fetchMock, "trips/5/special-catches", "POST")).toHaveLength(1);
  });

  it("doesn't save without the required fields", async () => {
    const fetchMock = renderTripForm("/trips/new");
    await userEvent.click(screen.getByText(t.trip.saveTrip));
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("loads the trip with its catches", async () => {
    const fetchMock = renderTripForm("/trips/5/edit");

    expect(await screen.findByText("Meeraal (Conger conger)")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: t.trip.editTrip })).toBeInTheDocument();
    expect(screen.getByDisplayValue("cool lake")).toBeInTheDocument();
    expect(screen.getByLabelText(t.trip.weatherElement.FOGGY)).toBeChecked();
    expect(screen.getByAltText(t.catches.photoOf("Meeraal (Conger conger)"))).toHaveAttribute("src", savedTrip.specialCatches[0].imageUrl);
    expect(requestBodies(fetchMock, "trips/5", "GET")).toHaveLength(1);
  });

  it("rejects an invalid trip id", async () => {
    const fetchMock = renderTripForm("/trips/abc/edit");
    expect(screen.getByText(t.trip.noTripId)).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("adds simple catches once and changes their amount", async () => {
    renderTripForm("/trips/5/edit");
    await addSimpleCatch();
    await addSimpleCatch("mutter");

    expect(document.querySelectorAll(".quantity-row")).toHaveLength(1);
    const row = simpleCatchRow();
    expect(within(row).getByText(aalmutterLabel)).toBeInTheDocument();
    expect(within(row).getByText("1")).toBeInTheDocument();

    await userEvent.click(within(row).getByText("+"));
    expect(within(row).getByText("2")).toBeInTheDocument();

    await userEvent.click(within(row).getByText("-"));
    await userEvent.click(within(row).getByText("-"));
    expect(screen.queryByText(aalmutterLabel)).not.toBeInTheDocument();
  });

  it("saves changed fields, simple catches and removed detailed catches", async () => {
    const fetchMock = renderTripForm("/trips/5/edit");
    const location = await screen.findByDisplayValue("cool lake");
    await userEvent.clear(location);
    await userEvent.type(location, "other lake");
    await addSimpleCatch();
    await userEvent.click(within(simpleCatchRow()).getByText("+"));
    await userEvent.click(screen.getByText(t.catches.deleteFish));
    expect(screen.queryByText("Meeraal (Conger conger)")).not.toBeInTheDocument();

    await userEvent.click(screen.getByText(t.trip.saveTrip));

    expect(await screen.findByText(homeText)).toBeInTheDocument();
    expect(requestBodies(fetchMock, "trips/5", "PUT")).toEqual([{
      location: "other lake", environment: "LAKE", time: "2026-04-05T06:30:00", hours: 3, temperature: null,
      waterLevel: null, weather: ["FOGGY"], notes: null,
      simpleCatches: [{ name: "Aalmutter", scientificName: "Zoarces viviparus", fishId: 1, amount: 2 }],
    }]);
    expect(requestBodies(fetchMock, "trips/5/special-catches/7", "DELETE")).toHaveLength(1);
  });

  it("uploads each new detailed catch with its image in its own request", async () => {
    const fetchMock = renderTripForm("/trips/5/edit");
    await addSpecialCatches(["aal", "mutter"]);
    // Size and weight input of each new catch
    const [firstSize, , secondSize] = document.querySelectorAll<HTMLInputElement>(".entry-list-item input[type=number]");
    await userEvent.type(firstSize, "30");
    await userEvent.type(secondSize, "45");
    const photo = new File(["png data"], "photo.png", { type: "image/png" });
    await userEvent.upload(fileInputs()[0], photo);
    expect(screen.getByAltText(t.catches.photoOf(aalmutterLabel))).toHaveAttribute("src", "blob:preview");

    await userEvent.click(screen.getByText(t.trip.saveTrip));

    expect(await screen.findByText(homeText)).toBeInTheDocument();
    const [first, second] = requestBodies(fetchMock, "trips/5/special-catches", "POST") as FormData[];
    expect(await catchPart(first)).toEqual({ fishId: 1, size: 30, weight: null, notes: null, name: "Aalmutter" });
    expect(first.get("image")).toBe(photo);
    expect(await catchPart(second)).toEqual({ fishId: 1, size: 45, weight: null, notes: null, name: "Aalmutter" });
    expect(second.get("image")).toBeNull();
  });

  it("keeps new detailed catches that couldn't be saved and doesn't create the trip twice", async () => {
    const fetchMock = renderTripForm("/trips/new", () => jsonResponse(uploadError, 415));
    await fillRequiredFields();
    await addSpecialCatches(["aal"]);

    await userEvent.click(screen.getByText(t.trip.saveTrip));

    expect(await screen.findByText(`UNSUPPORTED_IMAGE: ${uploadError.message}`)).toBeInTheDocument();
    expect(screen.queryByText(homeText)).not.toBeInTheDocument();
    expect(fileInputs()).toHaveLength(1);
    expect(screen.getByRole("heading", { name: aalmutterLabel })).toBeInTheDocument();

    await userEvent.click(screen.getByText(t.trip.saveTrip));

    expect(requestBodies(fetchMock, "trips", "POST")).toHaveLength(1);
    expect(requestBodies(fetchMock, "trips/5", "PUT")).toHaveLength(1);
  });

  it("only lets admins choose images", async () => {
    renderTripForm("/trips/new", undefined, false);
    await addSpecialCatches(["aal"]);

    expect(fileInputs()).toHaveLength(0);
    expect(screen.getByText(t.catches.photoUploadDisabled)).toBeInTheDocument();
  });

  it("rejects too large images before uploading", async () => {
    renderTripForm("/trips/new");
    await addSpecialCatches(["aal"]);

    const tooLarge = new File([new Uint8Array(MAX_IMAGE_BYTES + 1)], "huge.jpg", { type: "image/jpeg" });
    await userEvent.upload(fileInputs()[0], tooLarge);

    expect(screen.getByText(t.catches.imageTooLarge(10))).toBeInTheDocument();
    expect(screen.queryByAltText(t.catches.photoOf(aalmutterLabel))).not.toBeInTheDocument();
  });
});
