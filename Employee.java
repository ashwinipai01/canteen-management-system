public class Employee {
    String name;
    int age;
    long salary;

    public Employee(String name, int age, long salary) {
        this.name = name;
        this.age = age;
        this.salary = salary;
    }

    @Override
    public String toString() {
        return name + " - " + age + " - " + salary;
    }
}
