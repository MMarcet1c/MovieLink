(ns movielink.core
  (:require [movielink.db :as db]
            [next.jdbc :as jdbc]
            [buddy.hashers :as hashers]
            [clojure.string :as str]
            [movielink.genre :refer [genre-diff]]))

(def db-spec db/db-spec)

(defn create-user []
  (println "=== Create New User ===")
  (print "Enter username: ") (flush)
  (let [username (read-line)]
    ;; Check if username exists
    (if (seq (jdbc/execute! db-spec ["SELECT * FROM users WHERE username=?" username]))
      (do (println "Username already exists!") (create-user))
      (do
        ;; Password input
        (print "Enter password: ") (flush)
        (let [password (read-line)]
          (print "Confirm password: ") (flush)
          (let [confirm (read-line)]
            (if (= password confirm)
              (do
                ;; Insert user
                (jdbc/execute! db-spec
                               ["INSERT INTO users (username, password_hash) VALUES (?, ?)"
                                username (hashers/derive password)])
                (println "User created successfully! You are now logged in.")
                username) ;; return username
              (do
                (println "Passwords do not match. Try again.")
                (recur)))))))))

(defn login []
  (println "=== Login ===")
  (loop [attempts 0]
    (if (>= attempts 3)
      (do
        (println "You have reached the maximum number of login attempts. Exiting...")
        (System/exit 0))
      (do
        (print "Username: ") (flush)
        (let [username (read-line)]
          (print "Password: ") (flush)
          (let [password (read-line)
                user (first (jdbc/execute! db-spec ["SELECT * FROM users WHERE username=?" username]))]
            (if (and user (hashers/check password (:users/password_hash user)))
              (do
                (println "Login successful!")
                username)
              (do
                (println "Invalid username or password. Try again.")
                (recur (inc attempts))))))))))

(defn search-by-name []
  (print "Enter movie name or part of it: ") (flush)
  (let [input (read-line)
        movies (jdbc/execute! db-spec
                              ["SELECT DISTINCT * FROM movies WHERE LOWER(title) LIKE ?" (str "%" (str/lower-case input) "%")])]
    (if (empty? movies)
      (println "No movies found with that name.")
      (doseq [m movies]
        (println "-------------------------------")
        (println "Title:      " (:movies/title m))
        (println "Genres:     " (:movies/genres m))
        (println "Rating:     " (:movies/rating m))
        (println "Description:" (:movies/description m))
        (println "-------------------------------")))))

(defn search-by-rating []
  (print "Enter minimum rating: ") (flush)
  (let [rating (Double/parseDouble (read-line))
        movies (jdbc/execute! db-spec ["SELECT * FROM movies WHERE rating >= ?" rating])]
    (doseq [m movies]
      (println (:movies/title m) "(" (:movies/rating m) ") - " (:movies/genres m)))))

(defn list-genres []
  (let [movies (jdbc/execute! db-spec ["SELECT genres FROM movies"])
        genres (->> movies
                    (map :movies/genres)
                    (map #(str/split % #","))
                    (apply concat)
                    (map str/trim)
                    set)]
    (println "Available genres:")
    (doseq [g (sort genres)]
      (println "- " g))
    genres))

(defn search-by-genre []
  (let [all-genres (->> (jdbc/execute! db-spec ["SELECT genres FROM movies"])
                        (map :movies/genres)
                        (map #(str/split % #","))
                        (apply concat)
                        (map str/trim)
                        set)]
    ;; Print available genres
    (println "Available genres:")
    (doseq [g (sort all-genres)]
      (println "- " g))

    ;; Print instructions
    (println "\nFilter options:")
    (println "- Single genre: (ex Action")
    (println "- AND multiple genres: (ex Action,Drama) (movies must include all listed genres)")
    (println "- OR multiple genres: (ex Action/Drama) (movies with any of the listed genres)\n")

    (print "Enter genre filter: ") (flush)
    (let [input (read-line)
          genres-and (if (str/includes? input ",")
                       (map str/trim (str/split input #","))
                       nil)
          genres-or  (if (str/includes? input "/")
                       (map str/trim (str/split input #"/"))
                       nil)
          movies (cond
                   genres-and
                   (let [query (str "SELECT * FROM movies WHERE "
                                    (str/join " AND " (map #(str "LOWER(genres) LIKE '%" (str/lower-case %) "%'") genres-and)))]
                     (jdbc/execute! db-spec [query]))

                   genres-or
                   (let [query (str "SELECT * FROM movies WHERE "
                                    (str/join " OR " (map #(str "LOWER(genres) LIKE '%" (str/lower-case %) "%'") genres-or)))]
                     (jdbc/execute! db-spec [query]))

                   :else
                   ;; single genre
                   (jdbc/execute! db-spec ["SELECT * FROM movies WHERE LOWER(genres) LIKE ?" (str "%" (str/lower-case input) "%")]))]
      (if (empty? movies)
        (println "No movies found for that genre filter.")
        (doseq [m movies]
          (println (:movies/title m) "(" (:movies/rating m) ") - " (:movies/genres m)))))))

(defn add-to-favorites [username]
  (print "Enter movie title to add to favorites: ") (flush)
  (let [title (read-line)
        user  (first (jdbc/execute! db-spec ["SELECT * FROM users WHERE username=?" username]))
        movie (first (jdbc/execute! db-spec ["SELECT * FROM movies WHERE LOWER(title) LIKE ?" (str "%" (str/lower-case title) "%")]))]
    (if (and user movie)
      (do
        (jdbc/execute! db-spec
                       ["INSERT OR IGNORE INTO favorites (user_id, movie_id) VALUES (?, ?)"
                        (:users/id user) (:movies/id movie)])
        (println (str "Movie '" (:movies/title movie) "' added to favorites!")))
      (println "Movie not found."))))

(defn remove-from-favorites [username]
  (print "Enter movie title to remove from favorites: ") (flush)
  (let [title (read-line)
        user  (first (jdbc/execute! db-spec ["SELECT * FROM users WHERE username=?" username]))
        movie (first (jdbc/execute! db-spec
                                    ["SELECT * FROM movies WHERE LOWER(title) LIKE ?" (str "%" (str/lower-case title) "%")]))]
    (if (and user movie)
      (let [result (jdbc/execute! db-spec
                                  ["DELETE FROM favorites WHERE user_id=? AND movie_id=?"
                                   (:users/id user) (:movies/id movie)])
            deleted-count (:next.jdbc/update-count (first result))]
        (if (pos? deleted-count)
          (println (str "Movie '" (:movies/title movie) "' removed from favorites!"))
          (println "That movie was not in your favorites.")))
      (println "Movie not found."))))


(defn show-favorites [username]
  (let [user (first (jdbc/execute! db-spec ["SELECT * FROM users WHERE username=?" username]))
        movies (jdbc/execute! db-spec
                              ["SELECT m.* FROM movies m
                                JOIN favorites f ON m.id = f.movie_id
                                WHERE f.user_id = ?" (:users/id user)])]
    (if (empty? movies)
      (println "You have no favorites yet.")
      (doseq [m movies]
        (println "------------------------------------------------------------------------------------------")
        (print "Title: " (:movies/title m) "  |")
        (print "  Genres: " (:movies/genres m) "  |")
        (println "Rating: " (:movies/rating m) "  |")
        (println "------------------------------------------------------------------------------------------")))))

(defn favorites-stats [username]
  (let [user (first (jdbc/execute! db-spec ["SELECT * FROM users WHERE username=?" username]))
        movies (jdbc/execute! db-spec
                              ["SELECT m.* FROM movies m
                                JOIN favorites f ON m.id = f.movie_id
                                WHERE f.user_id = ?" (:users/id user)])]
    (if (empty? movies)
      (println "You have no favorites yet.")
      (let [total (count movies)
            all-genres (->> movies
                            (map :movies/genres)
                            (map #(str/split % #","))
                            (apply concat)
                            (map str/trim))
            genre-counts (frequencies all-genres)
            most-genre (first (apply max-key val genre-counts))
            avg-rating (/ (reduce + (map :movies/rating movies)) total)]

        (println "=== Favorites Statistics ===")
        (println "Total favorite movies:" total)
        (println "Most frequent genre:" most-genre)
        (println "Percentage per genre:")
        (doseq [[genre count] genre-counts]
          (let [pct (* 100 (/ count total))]
            (println (format "- %s: %.2f%%" genre (double pct)))))
        (println (format "Average rating: %.2f" avg-rating))))))

(defn add-friend [username]
  (print "Enter the username of the friend to add: ") (flush)
  (let [friend-name (read-line)
        user (first (jdbc/execute! db-spec ["SELECT * FROM users WHERE username=?" username]))
        friend (first (jdbc/execute! db-spec ["SELECT * FROM users WHERE username=?" friend-name]))]
    (cond
      (nil? friend) (println "User not found.")
      (= (:users/id user) (:users/id friend)) (println "You cannot add yourself as a friend.")
      :else
      (do
        ;; Insert friendship in both directions
        (jdbc/execute! db-spec
                       ["INSERT OR IGNORE INTO friends (user_id, friend_id) VALUES (?, ?)"
                        (:users/id user) (:users/id friend)])
        (jdbc/execute! db-spec
                       ["INSERT OR IGNORE INTO friends (user_id, friend_id) VALUES (?, ?)"
                        (:users/id friend) (:users/id user)])
        (println (str "You are now friends with " friend-name "!"))))))

(defn remove-friend [username]
  (print "Enter the username of the friend to remove: ") (flush)
  (let [friend-name (read-line)
        user (first (jdbc/execute! db-spec ["SELECT * FROM users WHERE username=?" username]))
        friend (first (jdbc/execute! db-spec ["SELECT * FROM users WHERE username=?" friend-name]))]
    (if (and friend user)
      (do
        (jdbc/execute! db-spec
                       ["DELETE FROM friends WHERE user_id=? AND friend_id=?"
                        (:users/id user) (:users/id friend)])
        (jdbc/execute! db-spec
                       ["DELETE FROM friends WHERE user_id=? AND friend_id=?"
                        (:users/id friend) (:users/id user)])
        (println (str "You are no longer friends with " friend-name)))
      (println "User not found."))))

(defn list-friends [username]
  (let [user (first (jdbc/execute! db-spec ["SELECT * FROM users WHERE username=?" username]))
        friends (jdbc/execute! db-spec
                               ["SELECT u.username FROM users u
                                 JOIN friends f ON u.id = f.friend_id
                                 WHERE f.user_id=?" (:users/id user)])]
    (if (empty? friends)
      (println "You have no friends yet.")
      (do
        (println "=== Your Friends ===")
        (doseq [f friends]
          (println "- " (:users/username f)))))))

(defn find-friends [username]
  (let [user (first (jdbc/execute! db-spec ["SELECT * FROM users WHERE username=?" username]))
        movies (jdbc/execute! db-spec
                              ["SELECT m.* FROM movies m
                                JOIN favorites f ON m.id = f.movie_id
                                WHERE f.user_id=?" (:users/id user)])]
    (if (empty? movies)
      (println "You have no favorites, cannot find friends.")
      (let [all-genres      (->> movies
                                 (map :movies/genres)
                                 (map #(str/split % #","))
                                 (apply concat)
                                 (map str/trim))
            genre-counts    (frequencies all-genres)
            most-genre      (first (apply max-key val genre-counts))
            candidate-users (jdbc/execute! db-spec
                                           ["SELECT DISTINCT u.id, u.username
                                             FROM users u
                                             JOIN favorites f ON u.id = f.user_id
                                             JOIN movies m ON m.id = f.movie_id
                                             WHERE LOWER(m.genres) LIKE ?
                                               AND u.username != ?
                                               AND u.id NOT IN (
                                                 SELECT friend_id FROM friends WHERE user_id = ?
                                               )"
                                            (str "%" (str/lower-case most-genre) "%")
                                            username
                                            (:users/id user)])
            added? (atom 0)]
        (if (empty? candidate-users)
          (println "No new users can be found with the same most frequent genre.")
          (do
            (println "Users with same favorite genre (" most-genre "):")
            (doseq [u candidate-users]
              (println "- " (:users/username u))
              (print "Add as friend? (y/n): ") (flush)
              (let [resp (str/lower-case (read-line))]
                (when (= resp "y")
                  (jdbc/execute! db-spec
                                 ["INSERT OR IGNORE INTO friends (user_id, friend_id) VALUES (?, ?)"
                                  (:users/id user) (:users/id u)])
                  (jdbc/execute! db-spec
                                 ["INSERT OR IGNORE INTO friends (user_id, friend_id) VALUES (?, ?)"
                                  (:users/id u) (:users/id user)])
                  (swap! added? inc)
                  (println "Added as friend!"))))
            (println (str @added? " friends added.")))
          )))))

(defn get-all-movies []
  (jdbc/execute! db-spec ["SELECT id, title, genres, rating, description FROM movies"]))

(defn parse-genres [genres-str]
  (map str/trim (str/split genres-str #",")))

(defn similarity-score [desired-rating desired-genre rating-weight genre-weight movie]
  (let [r-diff (Math/abs (- desired-rating (:movies/rating movie)))
        movie-genres (map str/lower-case (parse-genres (:movies/genres movie)))
        g-diff (apply min (map #(genre-diff desired-genre %) movie-genres))]
    (+ (* rating-weight r-diff)
       (* genre-weight g-diff))))

(defn best-match [desired-rating desired-genre rating-weight genre-weight]
  (let [movies (get-all-movies)]
    (apply min-key
           #(similarity-score desired-rating desired-genre rating-weight genre-weight %)
           movies)))

(defn recommend-movie []
  (print "Enter desired rating (e.g. 8.0): ") (flush)
  (let [desired-rating (Double/parseDouble (read-line))]
    (print "Enter desired genre: ") (flush)
    (let [desired-genre (str/lower-case (read-line))]
      (print "Enter rating weight (0-1): ") (flush)
      (let [rating-weight (Double/parseDouble (read-line))]
        (print "Enter genre weight (0-1): ") (flush)
        (let [genre-weight (Double/parseDouble (read-line))
              movie (best-match desired-rating desired-genre rating-weight genre-weight)]
          (println "\n=== Recommended Movie ===")
          (println "Title:" (:movies/title movie))
          (println "Genres:" (:movies/genres movie))
          (println "Rating:" (:movies/rating movie))
          (println "Description:" (:movies/description movie)))))))


(defn start-menu []
  (println "=== Welcome to Movie CLI ===")
  (println "1. Login")
  (println "2. Create New User")
  (println "3. Exit")
  (print "Choose option: ") (flush)
  (let [choice (read-line)]
    (case choice
      "1" (login)
      "2" (create-user)
      "3" (do (println "Goodbye!") (System/exit 0))
      (do (println "Invalid option") (recur)))))

(defn search-menu [username]
  (loop []
    (println "\n--- Search Movies ---")
    (println "1. Search by rating")
    (println "2. Search by genre(s)")
    (println "3. Search by name")
    (println "4. Recommend a movie for me")
    (println "0. Back")
    (print "Choose option: ") (flush)
    (let [choice (read-line)]
      (case choice
        "1" (do (search-by-rating) (recur))
        "2" (do (search-by-genre) (recur))
        "3" (do (search-by-name) (recur))
        "4" (do (recommend-movie) (recur))
        "0" :back
        (do (println "Invalid option.") (recur))))))

(defn favorites-menu [username]
  (loop []
    (println "\n--- Favorites movies ---")
    (println "1. Show Favorites movies")
    (println "2. Add movie to Favorites")
    (println "3. Remove movie from Favorites")
    (println "4. Favorites Statistics")
    (println "0. Back")
    (print "Choose option: ") (flush)
    (let [choice (read-line)]
      (case choice
        "1" (do (show-favorites username) (recur))
        "2" (do (add-to-favorites username) (recur))
        "3" (do (remove-from-favorites username) (recur))
        "4" (do (favorites-stats username) (recur))
        "0" :back
        (do (println "Invalid option.") (recur))))))

(defn friends-menu [username]
  (loop []
    (println "\n--- Friends ---")
    (println "1. List Friends")
    (println "2. Add Friend")
    (println "3. Remove Friend")
    (println "4. Find Friends (similar genre lovers)")
    (println "0. Back")
    (print "Choose option: ") (flush)
    (let [choice (read-line)]
      (case choice
        "1" (do (list-friends username) (recur))
        "2" (do (add-friend username) (recur))
        "3" (do (remove-friend username) (recur))
        "4" (do (find-friends username) (recur))
        "0" :back
        (do (println "Invalid option.") (recur))))))

(defn main-menu [username]
  (loop []
    (println "\n=== Movie CLI Menu ===")
    (println "1. Search movies")
    (println "2. Favorites movies")
    (println "3. Friends")
    (println "0. Exit")
    (print "Choose option: ") (flush)
    (let [choice (read-line)]
      (case choice
        "1" (do (search-menu username) (recur))
        "2" (do (favorites-menu username) (recur))
        "3" (do (friends-menu username) (recur))
        "0" (println "Goodbye!")
        (do (println "Invalid choice") (recur))))))

(defn -main []
  (db/setup-db)
  (let [user (start-menu)]
    (main-menu user)))
