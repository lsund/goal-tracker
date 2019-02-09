CREATE TABLE BOOK
(
    id              SERIAL PRIMARY KEY,
    title           VARCHAR(64) NOT NULL,
    done            BOOLEAN NOT NULL,
    doneDate        DATE
);

CREATE TABLE Goal
(
    id              SERIAL PRIMARY KEY,
    sequence        INT NOT NULL,
    description     VARCHAR(64) NOT NULL,
    deadline        DATE NOT NULL
);

CREATE TABLE Benefit
(
    id              SERIAL PRIMARY KEY,
    description     VARCHAR(64) NOT NULL,
    goalid          INT NOT NULL,
    FOREIGN KEY     (goalid) REFERENCES goal (id)
);

CREATE TABLE SubGoal
(
    id              SERIAL PRIMARY KEY,
    goalid          INT NOT NULL,
    description     VARCHAR(64) NOT NULL,
    deadline        DATE NOT NULL,
    done            BOOLEAN NOT NULL,
    FOREIGN KEY     (goalid) REFERENCES goal (id)
);

CREATE TABLE ActionItem
(
    id              SERIAL PRIMARY KEY,
    goalId          INT NOT NULL,
    description     VARCHAR(64) NOT NULL,
    FOREIGN KEY     (goalId) REFERENCES goal (id)
);

CREATE TABLE Iteration
(
    id              SERIAL PRIMARY KEY,
    startDate       DATE NOT NULL,
    endDate         DATE NOT NULL
);

CREATE TABLE Task
(
    id              SERIAL PRIMARY KEY,
    goalId          INT NOT NULL,
    iterationId     INT NOT NULL,
    actionItemId    INT NOT NULL,
    priority        VARCHAR(8),
    timeestimate    varchar(32),
    sequence        INT NOT NULL,

    description     VARCHAR(64) NOT NULL,
    current         INT NOT NULL,
    target          INT NOT NULL,
    unit            VARCHAR(8) NOT NULL,
    FOREIGN KEY     (goalId) REFERENCES goal (id),
    FOREIGN KEY     (actionItemId) REFERENCES ActionItem (id),
    FOREIGN KEY     (iterationId) REFERENCES iteration (id)
);

CREATE TABLE DoneTaskEntry
(
    id              SERIAL PRIMARY KEY,
    taskId          INT NOT NULL,
    taskType        INT NOT NULL,
    day             DATE NOT NULL
);
