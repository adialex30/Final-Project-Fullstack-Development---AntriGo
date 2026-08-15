/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // "Kunyit" (turmeric) gold — accent inspired by warung spice racks, not generic AI-orange.
        ember: {
          50: '#fdf8ec',
          100: '#faedc7',
          200: '#f4d78a',
          300: '#eebd4d',
          400: '#e6a52a',
          500: '#d4901c',
          600: '#b17114',
          700: '#8c5714',
          800: '#734617',
          900: '#5f3a18',
        },
        ink: {
          50: '#f6f5f4',
          100: '#e8e5e2',
          200: '#d1cbc4',
          300: '#aea59a',
          400: '#847a6d',
          500: '#655d52',
          600: '#4f4941',
          700: '#3d3832',
          800: '#28241f',
          900: '#1a1714',
        },
        paper: '#fbf7f0',
        chili: { 500: '#c8432f', 600: '#a83424' },
        sprout: { 500: '#4c8a5c', 600: '#3a6d47' },
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'system-ui', 'sans-serif'],
        display: ['"Fraunces"', 'serif'],
      },
    },
  },
  plugins: [],
}
