import { render, screen } from "@testing-library/react";
import Home from "@/app/page";

describe("Home", () => {
  it("renders the stage-one placeholder", () => {
    render(<Home />);
    expect(screen.getByRole("heading", { name: "图片素材制作网站" })).toBeInTheDocument();
  });
});
