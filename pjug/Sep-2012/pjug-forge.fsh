@/* This means less typing. If a script is automated, or is not meant to be interactive, use this command */;
set ACCEPT_DEFAULTS true;

@/* Create a new project */;
new-project --named pjug-todo --topLevelPackage org.jboss.pjug.todo --type war;

@/* Set up servlet, CDI, persistence and REST */;
servlet setup;
beans setup;
persistence setup --provider HIBERNATE --container JBOSS_AS7;
validation setup --provider HIBERNATE_VALIDATOR;

@/* Create a simple category entity */;
entity --named Project --package org.jboss.pjug.todo.model;
field string --named title;
constraint NotNull --onProperty title;

@/* Create a tag entity */;
entity --named Tag --package org.jboss.pjug.todo.model;
field string --named title;
constraint NotNull --onProperty title;

@/* Create the todo entity */;
entity --named Task;
field boolean --named completed --primitive;
field temporal --type DATE --named dueDate;
constraint Past --onProperty dueDate;
field string --named title;
constraint NotNull --onProperty title;
field string --named description;
field manyToOne --named project --fieldType ~.model.Project.java --inverseFieldName tasks;

cd ../Tag.java;
@/* Add a relationship between the tag and the task */;
field manyToMany --named tasks --fieldType ~.model.Task.java --inverseFieldName tags;

@/* Set up scaffolding for quick CRUD */;
faces setup;
scaffold setup --scaffoldType faces;
scaffold from-entity ~.model.* --scaffoldType faces --overwrite;

@/* Set up rest end points */;
rest setup;
rest endpoint-from-entity ~.model.*;

@/* Go home and build */;
cd ~~;
build;

@/* Reset */;
set ACCEPT_DEFAULTS false;
