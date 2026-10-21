import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { NotifiableContainer, type NotifiableContentContext, type WrappedComponent } from "../components/NotifiableContainer";
import { Loading } from "../components/Loading";
import type { GalleryCatchDto, GalleryCatchPageDto } from "../components/api/FishCatch";
import { useDebounce } from "../util/useDebounce";
import { useInfiniteList, type ListPage } from "../util/useInfiniteList";
import { buildCatchQuery, EMPTY_CATCH_SEARCH_PARAMS, type CatchSearchParams } from "../util/catchQuery";
import { fishLabel } from "../util/fishLabel";
import { useTranslation } from "../i18n/LanguageContext";
// For the search bar and trip detail rows, which look the same as on Home
import "./Home.css";
import "./Gallery.css";

const CATCH_PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 400;

const toCatchListPage = (catchPage: GalleryCatchPageDto): ListPage<GalleryCatchDto> =>
  ({ items: catchPage.catches, hasNext: catchPage.hasNext });

/**
 * All special catches of the user as a gallery of their photos, a tile expands to show the catch and its trip
 */
export default function Gallery() {
  const [searchParams, setSearchParams] = useState<CatchSearchParams>(EMPTY_CATCH_SEARCH_PARAMS);

  // Stable across renders so the search inputs (and their debounce timer) aren't remounted on every keystroke
  const OuterWrapper = useCallback(({ children }: WrappedComponent) => (
    <div className="main-flex-container full-page">
      <div className="search-bar">
        <GallerySearchBar onSearch={setSearchParams} />
      </div>
      {children}
    </div>
  ), []);

  // Remounts the grid on new search params, which starts the paging over
  const MainContent = useCallback((props: NotifiableContentContext) => (
    <CatchGrid {...props} searchParams={searchParams} />
  ), [searchParams]);

  return (
    <NotifiableContainer MainContent={MainContent} ContentWrapper={OuterWrapper} />
  );
}

function CatchGrid({ setError, searchParams }: NotifiableContentContext & { searchParams: CatchSearchParams }) {
  const pagePath = useCallback((page: number) =>
    `special-catches?${buildCatchQuery(page, CATCH_PAGE_SIZE, searchParams)}`, [searchParams]);
  const { items: catches, hasNext, loading, sentinelRef }
    = useInfiniteList<GalleryCatchDto, GalleryCatchPageDto>(pagePath, toCatchListPage, setError);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const expandedRef = useRef<HTMLDivElement | null>(null);
  const { t } = useTranslation();

  // Spanning the whole row may move the expanded tile down to the next row
  useEffect(() => {
    expandedRef.current?.scrollIntoView?.({ block: "nearest", behavior: "smooth" });
  }, [expandedId]);

  return (
    <div className="scroll-container">
      {loading && <Loading />}
      {!loading && !hasNext && catches.length === 0 && <p className="empty-state">{t.gallery.noCatches}</p>}
      <div className="gallery-grid">
        {catches.map((galleryCatch) => {
          const catchId = galleryCatch.specialCatch.catchId;
          return catchId === expandedId
            ? <ExpandedTile key={catchId} galleryCatch={galleryCatch} tileRef={expandedRef}
                onClose={() => setExpandedId(null)} />
            : <div className="gallery-tile" key={catchId}>
                <button type="button" className="gallery-tile-button" aria-expanded={false}
                  onClick={() => setExpandedId(catchId)}>
                  <CatchImage galleryCatch={galleryCatch} />
                  <span className="gallery-tile-caption">
                    {galleryCatch.specialCatch.name ?? galleryCatch.specialCatch.scientificName}
                  </span>
                </button>
              </div>
        })}
      </div>
      {hasNext && <div ref={sentinelRef} />}
    </div>
  );
}

function CatchImage({ galleryCatch }: { galleryCatch: GalleryCatchDto }) {
  const { t } = useTranslation();
  const { imageUrl, name, scientificName } = galleryCatch.specialCatch;
  return imageUrl
    ? <img className="gallery-image" src={imageUrl} alt={t.catches.photoOf(fishLabel(name, scientificName))}
        loading="lazy" />
    : <span className="gallery-image gallery-placeholder">{t.gallery.noPhoto}</span>
}

type ExpandedTileType = {
  galleryCatch: GalleryCatchDto,
  tileRef: React.RefObject<HTMLDivElement | null>,
  onClose: () => void,
}
function ExpandedTile({ galleryCatch, tileRef, onClose }: ExpandedTileType) {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { specialCatch, tripId } = galleryCatch;
  const [date, time] = galleryCatch.time.split("T");

  return (
    <div className="gallery-tile expanded" ref={tileRef}>
      <div className="gallery-expanded-image">
        {specialCatch.imageUrl
          ? <a href={specialCatch.imageUrl} target="_blank" rel="noreferrer" title={t.gallery.openPhoto}>
              <CatchImage galleryCatch={galleryCatch} />
            </a>
          : <CatchImage galleryCatch={galleryCatch} />
        }
      </div>
      <div className="gallery-expanded-info">
        <h3>{fishLabel(specialCatch.name, specialCatch.scientificName)}</h3>
        <div className="trip-detail"><span className="label">{t.catches.size}</span>
          <span>{specialCatch.size ?? "-"}</span>
        </div>
        <div className="trip-detail"><span className="label">{t.catches.weight}</span>
          <span>{specialCatch.weight ?? "-"}</span>
        </div>
        {specialCatch.notes && (
          <div className="gallery-notes">
            <span className="label">{t.catches.notes}</span>
            <p>{specialCatch.notes}</p>
          </div>
        )}
        <hr/>
        <span className="label section-title">{t.gallery.trip}</span>
        <div className="trip-detail"><span className="label">{t.trip.dateTime}</span>
          <span>{date} {time}</span>
        </div>
        <div className="trip-detail"><span className="label">{t.trip.location}</span>
          <span>{galleryCatch.location}</span>
        </div>
        <div className="trip-detail"><span className="label">{t.trip.environment}</span>
          <span>{galleryCatch.environment ? t.trip.environmentElement[galleryCatch.environment] : "-"}</span>
        </div>
        <div className="trip-detail"><span className="label">{t.trip.duration}</span>
          <span>{galleryCatch.hours ? t.trip.hours(galleryCatch.hours) : "-"}</span>
        </div>
        <div className="trip-detail"><span className="label">{t.trip.weather}</span>
          <span>{galleryCatch.weather.length > 0
            ? galleryCatch.weather.map(el => t.trip.weatherElement[el]).join(", ") : "-"}</span>
        </div>
        <div className="gallery-expanded-actions">
          <button type="button" onClick={() => navigate(`/trips/${tripId}/edit`)}>
            {t.trip.editTrip}
          </button>
          <button type="button" aria-expanded={true} onClick={onClose}>
            {t.gallery.close}
          </button>
        </div>
      </div>
    </div>
  );
}

function GallerySearchBar({ onSearch }: { onSearch: (searchParams: CatchSearchParams) => void }) {
  const [rawParams, setRawParams] = useState<CatchSearchParams>(EMPTY_CATCH_SEARCH_PARAMS);
  const debouncedParams = useDebounce<CatchSearchParams>(rawParams, SEARCH_DEBOUNCE_MS);
  const { t } = useTranslation();

  useEffect(() => {
    onSearch(debouncedParams);
    // onSearch is the setSearchParams setter from Gallery, stable across renders
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedParams])

  return (
    <div className="trip-search-bar">
      <input
        type="text"
        placeholder={t.gallery.searchSpecies}
        value={rawParams.species}
        onChange={(e) => setRawParams({ ...rawParams, species: e.target.value })}
        className="search-input"
      />
      <input
        type="text"
        placeholder={t.trip.searchLocation}
        value={rawParams.location}
        onChange={(e) => setRawParams({ ...rawParams, location: e.target.value })}
        className="search-input"
      />
      <label className="form-label">{t.trip.from}</label>
      <input
        type="date"
        value={rawParams.from}
        onChange={(e) => setRawParams({ ...rawParams, from: e.target.value })}
      />
      <label className="form-label">{t.trip.to}</label>
      <input
        type="date"
        value={rawParams.to}
        onChange={(e) => setRawParams({ ...rawParams, to: e.target.value })}
      />
      <label className="form-label">{t.gallery.minSize}</label>
      <input
        className="trip-number-input"
        type="number"
        min="0"
        value={rawParams.minSize}
        onChange={(e) => setRawParams({ ...rawParams, minSize: e.target.value })}
      />
      <label className="form-label">{t.gallery.minWeight}</label>
      <input
        className="trip-number-input"
        type="number"
        min="0"
        value={rawParams.minWeight}
        onChange={(e) => setRawParams({ ...rawParams, minWeight: e.target.value })}
      />
      <label className="gallery-checkbox">
        <input
          type="checkbox"
          checked={rawParams.withImage}
          onChange={(e) => setRawParams({ ...rawParams, withImage: e.target.checked })}
        />
        {t.gallery.withImage}
      </label>
    </div>
  );
}
