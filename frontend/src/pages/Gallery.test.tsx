import { beforeEach, describe, expect, it } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import Gallery from './Gallery';
import { jsonResponse, mockFetch } from '../test/mockFetch';
import { mockIntersectionObserver } from '../test/mockIntersectionObserver';
import type { GalleryCatchDto, GalleryCatchPageDto } from '../components/api/FishCatch';
import { translations } from '../i18n/LanguageContext';

const t = translations.de;

const withPhoto: GalleryCatchDto = {
  specialCatch: { catchId: 7, fishId: 2, imageUrl: "/api/trips/5/special-catches/7/image", size: 80,
    weight: 2000, notes: "big one", name: "Meeraal", scientificName: "Conger conger" },
  tripId: 5,
  location: "cool lake",
  environment: "LAKE",
  time: "2026-04-05T06:30:00",
  hours: 3,
  weather: ["FOGGY"],
};

const withoutPhoto: GalleryCatchDto = {
  ...withPhoto,
  specialCatch: { ...withPhoto.specialCatch, catchId: 8, imageUrl: null, name: null,
    scientificName: "Zoarces viviparus" },
  tripId: 6,
  location: "river bend",
};

const editText = "trip editor";

function renderGallery(page: GalleryCatchPageDto) {
  const fetchMock = mockFetch({ "GET special-catches": () => jsonResponse(page) });
  render(
    <MemoryRouter initialEntries={["/catches"]}>
      <Routes>
        <Route path="/catches" element={<Gallery />} />
        <Route path="/trips/:id/edit" element={<p>{editText}</p>} />
      </Routes>
    </MemoryRouter>
  );
  return fetchMock;
}

describe("Gallery", () => {
  beforeEach(() => {
    mockIntersectionObserver();
  });

  it("shows a tile per catch, with a placeholder for catches without photo", async () => {
    renderGallery({ catches: [withPhoto, withoutPhoto], hasNext: false, totalElements: 2 });

    expect(await screen.findByAltText(t.catches.photoOf("Meeraal (Conger conger)"))).toHaveAttribute(
      "src", withPhoto.specialCatch.imageUrl);
    expect(screen.getByText("Zoarces viviparus")).toBeInTheDocument();
    expect(screen.getByText(t.gallery.noPhoto)).toBeInTheDocument();
    // Trip info is only shown once a tile is expanded
    expect(screen.queryByText("cool lake")).not.toBeInTheDocument();
  });

  it("expands a tile with the catch and trip info, and links to the trip editor", async () => {
    renderGallery({ catches: [withPhoto, withoutPhoto], hasNext: false, totalElements: 2 });

    await userEvent.click(await screen.findByText("Meeraal"));
    expect(screen.getByText("Meeraal (Conger conger)")).toBeInTheDocument();
    expect(screen.getByText("big one")).toBeInTheDocument();
    expect(screen.getByText("cool lake")).toBeInTheDocument();
    expect(screen.getByText("2026-04-05 06:30:00")).toBeInTheDocument();
    expect(screen.getByText(t.trip.environmentElement.LAKE)).toBeInTheDocument();
    expect(screen.getByText(t.trip.weatherElement.FOGGY)).toBeInTheDocument();
    expect(screen.queryByText("river bend")).not.toBeInTheDocument();

    await userEvent.click(screen.getByText(t.trip.editTrip));
    expect(screen.getByText(editText)).toBeInTheDocument();
  });

  it("collapses the expanded tile again", async () => {
    renderGallery({ catches: [withPhoto], hasNext: false, totalElements: 1 });

    await userEvent.click(await screen.findByText("Meeraal"));
    await userEvent.click(screen.getByText(t.gallery.close));
    expect(screen.queryByText("cool lake")).not.toBeInTheDocument();
  });

  it("searches with the entered species", async () => {
    const fetchMock = renderGallery({ catches: [], hasNext: false, totalElements: 0 });

    expect(await screen.findByText(t.gallery.noCatches)).toBeInTheDocument();
    await userEvent.type(screen.getByPlaceholderText(t.gallery.searchSpecies), "aal");
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/special-catches?page=0&size=20&species=aal", expect.anything()));
  });
});
