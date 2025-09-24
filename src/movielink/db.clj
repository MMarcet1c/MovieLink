(ns movielink.db
  (:require [next.jdbc :as jdbc]
            [buddy.hashers :as hashers])
  (:import [java.io File]))

(def db-file "movies.db")

(def db-spec {:dbtype "sqlite" :dbname db-file})

(defn db-exists? []
  (.exists (File. db-file)))

(defn setup-db []
  (when (not (db-exists?))
    (println "Creating database and seeding data...")
    ;; Create tables
    (jdbc/execute! db-spec ["CREATE TABLE IF NOT EXISTS users (
                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                               username TEXT UNIQUE,
                               password_hash TEXT)"])
    (jdbc/execute! db-spec ["CREATE TABLE IF NOT EXISTS movies (
                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                               title TEXT UNIQUE,
                               genres TEXT,
                               rating REAL,
                               description TEXT)"])
    (jdbc/execute! db-spec ["CREATE TABLE IF NOT EXISTS favorites (
                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                               user_id INTEGER,
                               movie_id INTEGER,
                               UNIQUE(user_id, movie_id),
                               FOREIGN KEY(user_id) REFERENCES users(id),
                               FOREIGN KEY(movie_id) REFERENCES movies(id))"])

    ;; Seed data
    (jdbc/execute! db-spec
                   ["INSERT OR IGNORE INTO users (username, password_hash) VALUES (?, ?)"
                    "admin" (hashers/derive "admin")])

    (doseq [movie [["The Matrix" "Action,Sci-Fi" 8.7 "A hacker discovers reality is simulated."]
                   ["Inception" "Action,Sci-Fi,Thriller" 8.8 "A thief steals secrets through dreams."]
                   ["Titanic" "Romance,Drama" 7.8 "Love story on the ill-fated ship."]
                   ["The Godfather" "Crime,Drama" 9.2 "Mafia family saga."]
                   ["Interstellar" "Sci-Fi,Adventure,Drama" 8.6 "Space journey to save humanity."]]]
      (jdbc/execute! db-spec
                     ["INSERT OR IGNORE INTO movies (title, genres, rating, description) VALUES (?, ?, ?, ?)"
                      (nth movie 0) (nth movie 1) (nth movie 2) (nth movie 3)]))))
