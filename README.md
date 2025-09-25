# Movie CLI Application

A **Clojure-based Command Line Interface (CLI) application** for managing and exploring movies, gathering favorite movies in one place, and social connections between movies lovers. This app allows users to login, search for movies, manage favorites, and discover friends based on shared movie tastes.

---

## Features

### User Management
- **Login** with username and password.
- **Create new user** with password confirmation.
- Passwords are securely hashed using `buddy-hashers`.

### Movie Search
- **Search by rating** – find movies with ratings above a specified threshold.
- **Search by genre** – filter movies by single or multiple genres.
- **Search by name** – search for movies by partial or full title.
- **Recommendation** – find the movie that best matches your preferences based on rating and genre similarity.

### Favorites
- **Add movie to favorites** for your profile.
- **Remove movie from favorites**.
- **Show favorite movies**.
- **Favorites statistics**:
    - Total favorite movies
    - Most frequent genre
    - Percentage of each genre in favorites
    - Average rating of favorites movies

### Friends
- **Add and remove friends** by username.
- **List all friends**.
- **Find friends** who share your most frequent genre.
- Interactive friend suggestions: add or skip suggested users.

### Menu Navigation
- Clean **three-section menu**:
    1. Search movies
    2. Favorites movies
    3. Friends
- Each section has sub-options with **back navigation**.
- CLI-based prompts for all inputs.

---

## Database

The application uses **SQLite** (`movies.db`) for persistent storage.

### Tables

- **users** – stores usernames and hashed passwords.
- **movies** – stores movie details: `title`, `genres`, `rating`, `description`.
- **favorites** – links users to their favorite movies.
- **friends** – links users with their friends.

---

## Dependencies

- **Clojure** – Programming language
- **Leiningen** – Build tool
- **next.jdbc** – Database access
- **buddy-hashers** – Password hashing
- **SQLite JDBC** – Database driver

---

## Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/MMarcet1c/MovieLink.git
   cd MovieLink
   lein run


## License

Copyright © 2025 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
