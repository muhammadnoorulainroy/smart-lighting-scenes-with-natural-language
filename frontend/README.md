# Smart Lighting Scenes - Frontend

A modern, beautiful Vue 3 frontend for the Smart Lighting Scenes application with Google OAuth authentication.

## Features

- **Beautiful Landing Page** - Modern, professional design with gradients and animations
- **Google OAuth Authentication** - Secure sign-in with Google
- **Real-time Updates** - WebSocket integration for live device updates
- **Responsive Design** - Works perfectly on desktop, tablet, and mobile
- **Dark Mode** - System-aware dark mode support
- **Natural Language Control** - AI-powered lighting control interface
- **Smart Routing** - Protected routes with authentication guards

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend server running on `http://localhost:8080`

### Installation

1. Install dependencies:
```bash
npm install
```

2. Configure environment variables:
```bash
cp .env.example .env
```

Edit `.env` to match your backend configuration:
```env
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
```

3. Start the development server:
```bash
npm run dev
```

The app will be available at `http://localhost:5173`

### Build for Production

```bash
npm run build
```

Preview the production build:
```bash
npm run preview
```

## Project Structure

```
frontend/
├── src/
│   ├── api/              # API clients and services
│   │   ├── axios.js      # Axios configuration with interceptors
│   │   ├── auth.js       # Authentication API
│   │   └── rooms.js      # Rooms API (example)
│   ├── assets/           # Static assets
│   │   └── main.css      # Global styles with Tailwind
│   ├── components/       # Reusable Vue components
│   │   ├── AuthButton.vue         # Google sign-in button
│   │   ├── UserMenu.vue           # User profile dropdown
│   │   ├── LoadingSpinner.vue     # Loading indicator
│   │   └── ErrorAlert.vue         # Error notifications
│   ├── router/           # Vue Router configuration
│   │   └── index.js      # Routes with guards
│   ├── stores/           # Pinia state management
│   │   └── auth.js       # Authentication store
│   ├── utils/            # Utility functions
│   │   └── routeGuards.js # Route protection logic
│   ├── views/            # Page components
│   │   ├── HomeView.vue           # Landing page
│   │   ├── AuthCallbackView.vue   # OAuth callback handler
│   │   ├── DashboardView.vue      # Main dashboard
│   │   ├── ScenesView.vue         # Scenes management
│   │   ├── RoutinesView.vue       # Routines management
│   │   ├── SchedulesView.vue      # Schedules management
│   │   └── NotFoundView.vue       # 404 page
│   ├── App.vue           # Root component
│   └── main.js           # Application entry point
├── public/               # Public static files
├── index.html            # HTML template
├── vite.config.js        # Vite configuration
├── tailwind.config.js    # Tailwind CSS configuration
├── postcss.config.js     # PostCSS configuration
└── package.json          # Dependencies and scripts
```

## Design System

### Color Palette

The application uses a warm, modern color scheme (avoiding purple and blue as requested):

- **Primary** - Yellow/Amber tones (`#eab308`)
- **Accent** - Green tones (`#22c55e`)
- **Neutral** - Grayscale for text and backgrounds
- **Additional** - Orange, Red, Emerald, Cyan for features

### Typography

- **Display Font**: Outfit - Used for headings
- **Body Font**: Inter - Used for body text

### Components

All components follow these conventions:
- Consistent spacing using Tailwind's scale
- Hover states for interactive elements
- Smooth transitions and animations
- Dark mode support
- Accessible keyboard navigation

## Authentication Flow

1. User clicks "Sign in with Google" button
2. Redirected to Google OAuth consent screen
3. After consent, Google redirects to backend `/login/oauth2/code/google`
4. Backend processes OAuth and sets session cookie
5. Backend redirects to frontend `/auth/callback?success=true`
6. Frontend callback page verifies authentication
7. User is redirected to dashboard or intended page

### Protected Routes

Routes requiring authentication:
- `/dashboard` - Main control panel
- `/scenes` - Scene management
- `/routines` - Routine management
- `/schedules` - Schedule management

### Route Guards

```javascript
import { requireAuth } from '@/utils/routeGuards'

{
  path: '/dashboard',
  component: DashboardView,
  beforeEnter: requireAuth
}
```

## Development

### Code Style

- ESLint for linting
- Prettier for formatting

Run linter:
```bash
npm run lint
```

Format code:
```bash
npm run format
```

### State Management

Using Pinia for state management. Example auth store:

```javascript
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
authStore.login()
authStore.logout()
```

### API Calls

All API calls go through the configured axios instance:

```javascript
import apiClient from '@/api/axios'

const response = await apiClient.get('/api/rooms')
```

### Adding New Routes

1. Create view component in `src/views/`
2. Add route in `src/router/index.js`
3. Add route guard if needed
4. Update navigation in `App.vue` or `UserMenu.vue`

## API Integration

### Base Configuration

API URL is configured in `.env`:
```env
VITE_API_URL=http://localhost:8080
```

### Authentication

Session-based authentication using HTTP-only cookies:
- Cookies are automatically sent with requests
- CORS is configured to allow credentials
- Sessions expire based on backend configuration

### WebSocket

For real-time updates (to be implemented):
```javascript
import { io } from 'socket.io-client'

const socket = io(import.meta.env.VITE_WS_URL, {
  withCredentials: true
})
```

## Responsive Design

The application is fully responsive with breakpoints:
- Mobile: < 640px
- Tablet: 640px - 1024px
- Desktop: > 1024px

## Accessibility

- Semantic HTML
- ARIA labels for interactive elements
- Keyboard navigation support
- Focus indicators
- Screen reader friendly

## Performance

- Lazy loading for route components
- Optimized images and assets
- Tree-shaking with Vite
- Code splitting
- CSS optimization with Tailwind

## License

This project is part of the Smart Lighting Scenes application.

## Contributing

1. Follow the existing code style
2. Write meaningful commit messages
3. Test thoroughly before committing
4. Update documentation as needed

## Known Issues

None at the moment.

## Support

For issues and questions, please refer to the main project documentation.






