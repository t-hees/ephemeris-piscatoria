import { Route, Routes } from 'react-router-dom'
import FishSearch from './components/FishSearch.tsx'
import LoginPage from './pages/LoginPage.tsx'
import Navbar from './components/Navbar.tsx'
import UserPage from './pages/UserPage.tsx'
import Home from './pages/Home.tsx'
import TripForm from './pages/TripForm.tsx'
import Gallery from './pages/Gallery.tsx'
import { useEffect } from 'react'
import { useAuth } from './util/AuthContext.tsx'

export default function App() {

  const {isAuthenticated, fetchName} = useAuth();

  useEffect(() => {
    fetchName();
  }, [fetchName]);

  return (
    <>
      <Navbar />
      <div id="main-content-container">
        <Routes>
          {isAuthenticated()
            && (
              <>
                <Route path="/" element={<Home />} />
                <Route path="/catches" element={<Gallery />} />
                <Route path="/fish-search" element={<FishSearch />} />
                <Route path="/user" element={<UserPage />} />
                <Route path="/trips/new" element={<TripForm />} />
                <Route path="/trips/:id/edit" element={<TripForm />} />
              </>
            )
          }
          <Route path="/*" element={<LoginPage />} />
        </Routes>
      </div>
    </>
  )
}
