import { BrowserRouter, Routes, Route } from "react-router-dom";
import MainLayout from "./layouts/MainLayout";
import HomePage from "./pages/Home/HomePage";
import ActusPage from "./pages/Actus/ActusPage";
import ArticlePage from "./pages/Actus/ArticlePage";
import AboutPage from "./pages/About/AboutPage";
import LoginPage from './pages/Login/LoginPage';
import RegisterPage from "./pages/Register/RegisterPage";
import DashboardPage from "./pages/Dashboard/DashboardPage";
import VerificationPage from "./pages/Verification/VerificationPage";
import SearchTripsPage from "./pages/Trips/SearchTripsPage";
import CreateTripPage from "./pages/Trips/CreateTripPage";
import TripDetailPage from "./pages/Trips/TripDetailPage";
import MyBookingsPage from "./pages/Bookings/MyBookingsPage";
import StyleguidePage from "./pages/Styleguide/StyleguidePage";
import MentionsLegalesPage from "./pages/Legal/MentionsLegalesPage";
import ConfidentialitePage from "./pages/Legal/ConfidentialitePage";
import CguPage from "./pages/Legal/CguPage";
import CookiesPage from "./pages/Legal/CookiesPage";
import EntreprisesPage from "./pages/Entreprises/EntreprisesPage";
import BlogPage from "./pages/Blog/BlogPage";
import BlogPostPage from "./pages/Blog/BlogPostPage";
import NotFoundPage from "./pages/NotFound/NotFoundPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Le Layout englobe toutes ces routes */}
        <Route path="/" element={<MainLayout />}>
          <Route index element={<HomePage />} />
          <Route path="entreprises" element={<EntreprisesPage />} />
          <Route path="actus" element={<ActusPage />} />
          {/* Doit rester APRES la route fixe "actus". */}
          <Route path="actus/:id" element={<ArticlePage />} />
          <Route path="a-propos" element={<AboutPage />} />
          <Route path="blog" element={<BlogPage />} />
          {/* Doit rester APRES la route fixe "blog". */}
          <Route path="blog/:slug" element={<BlogPostPage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="verify-email" element={<VerificationPage />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="trips/search" element={<SearchTripsPage />} />
          <Route path="trips/create" element={<CreateTripPage />} />
          {/* Doit rester APRÈS les routes fixes, sinon "search" et "create"
              seraient interprétés comme des identifiants de trajet. */}
          <Route path="trips/:uuid" element={<TripDetailPage />} />
          <Route path="bookings" element={<MyBookingsPage />} />
          {/* Documents légaux. Toujours accessibles, y compris hors connexion :
              on ne peut pas exiger d'accepter des conditions qu'il faudrait un
              compte pour lire. */}
          <Route path="mentions-legales" element={<MentionsLegalesPage />} />
          <Route path="confidentialite" element={<ConfidentialitePage />} />
          <Route path="cgu" element={<CguPage />} />
          <Route path="cookies" element={<CookiesPage />} />

          {/* Planche des composants : reference visuelle et support du rapport. */}
          <Route path="styleguide" element={<StyleguidePage />} />

          {/* Dernière route, et à l'intérieur du layout : une adresse inconnue
              doit rendre l'en-tête et le pied de page, sans quoi le visiteur se
              retrouve sur un écran nu, sans navigation pour en sortir. Sans
              cette route, React Router ne trouve rien à rendre dans l'Outlet et
              affiche une page vide entre les deux. */}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
