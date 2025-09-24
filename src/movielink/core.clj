(ns movielink.core
  (:require [movielink.db :as db]
            [next.jdbc :as jdbc]
            [buddy.hashers :as hashers]
            [clojure.string :as str]))

(def db-spec db/db-spec)

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

(defn menu [username]
  (println "\n=== Movie CLI Menu ===")
  (println "1. Search by rating")
  (println "2. Search by genre")
  (println "3. Search by name")
  (println "4. Show Favorites")
  (println "5. Add movie to Favorites")
  (println "6. Exit")
  (print "Choose option: ") (flush)
  (let [choice (read-line)]
    (case choice
      "1" (do (search-by-rating) (menu username))
      "2" (do (search-by-genre) (menu username))
      "3" (do (search-by-name) (menu username))
      "4" (do (show-favorites username) (menu username))
      "5" (do (add-to-favorites username) (menu username))
      "6" (println "Goodbye!")
      (do (println "Invalid choice") (menu username)))))


(defn -main []
  (db/setup-db)
  (let [user (login)]
    (menu user)))
