CREATE TABLE Goal
(
    id              SERIAL PRIMARY KEY,
    description     VARCHAR(64) NOT NULL,
    deadline        DATE NOT NULL
);

CREATE TABLE ActionItem
(
    id              SERIAL PRIMARY KEY,
    goalId          INT NOT NULL,
    description     VARCHAR(64) NOT NULL,
    FOREIGN KEY     (goalid) REFERENCES goal (id)
);

CREATE TABLE Iteration
(
    id              SERIAL PRIMARY KEY,
    startDate       DATE NOT NULL,
    endDate         DATE NOT NULL
);

CREATE TABLE TimedTask
(
    id              SERIAL PRIMARY KEY,
    description     VARCHAR(64) NOT NULL,
    current         INT NOT NULL,
    goal            INT NOT NULL,
    unit            VArCHAR(8) NOT NULL
);
