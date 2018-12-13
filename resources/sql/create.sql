CREATE TABLE BOOK
(
    id              SERIAL PRIMARY KEY,
    title           VARCHAR(64) NOT NULL,
    done            BOOLEAN NOT NULL
);

CREATE TABLE Goal
(
    id              SERIAL PRIMARY KEY,
    sequence        INT NOT NULL,
    description     VARCHAR(64) NOT NULL,
    deadline        DATE NOT NULL
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

CREATE TABLE IncrementalTask
(
    id              SERIAL PRIMARY KEY,
    goalId          INT NOT NULL,
    iterationId     INT NOT NULL,
    actionItemId    INT NOT NULL,
    description     VARCHAR(64) NOT NULL,
    current         INT NOT NULL,
    target          INT NOT NULL,
    unit            VARCHAR(8) NOT NULL,
    priority        VARCHAR(8),
    sequence        INT NOT NULL,
    FOREIGN KEY     (goalId) REFERENCES goal (id),
    FOREIGN KEY     (actionItemId) REFERENCES ActionItem (id),
    FOREIGN KEY     (iterationId) REFERENCES iteration (id)
);

CREATE TABLE CheckedTask
(
    id              SERIAL PRIMARY KEY,
    goalId          INT NOT NULL,
    iterationId     INT NOT NULL,
    actionItemId    INT NOT NULL,
    description     VARCHAR(64) NOT NULL,
    done            BOOLEAN NOT NULL,
    priority        VARCHAR(8),
    sequence        INT NOT NULL,
    FOREIGN KEY     (goalId) REFERENCES goal (id),
    FOREIGN KEY     (actionItemId) REFERENCES ActionItem (id),
    FOREIGN KEY     (iterationId) REFERENCES iteration (id)
);

CREATE TABLE ReadingTask
(
    id              SERIAL PRIMARY KEY,
    goalId          INT NOT NULL,
    iterationId     INT NOT NULL,
    actionItemId    INT NOT NULL,
    BookId          INT NOT NULL,
    page            INT NOT NULL,
    done            BOOLEAN NOT NULL,
    priority        VARCHAR(8),
    sequence        INT NOT NULL,
    FOREIGN KEY     (goalId) REFERENCES goal (id),
    FOREIGN KEY     (actionItemId) REFERENCES ActionItem (id),
    FOREIGN KEY     (iterationId) REFERENCES iteration (id),
    FOREIGN KEY     (bookId) REFERENCES Book (id)
);

CREATE TABLE TaskUpdate
(
    id              SERIAL PRIMARY KEY,
    taskId          INT NOT NULL,
    taskType        INT NOT NULL,
    day             DATE NOT NULL
);
