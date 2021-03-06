/*
 * Team 4454 FRC 2017 FIRST STEAMWORKS
 * 
 * http://slarobotics.org
 */


package org.usfirst.frc.team4454.robot;

import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Servo;
import edu.wpi.first.wpilibj.Victor;

import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.ControlMode;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.CounterBase;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.vision.VisionThread;

public class Robot extends IterativeRobot {
	
	private static final double SHOOTER_RPM_PRE_AUTON=5000.0;
	private static final double SHOOTER_RPM_PRE_TELEOP=4000.0;
	
	Joystick leftStick;  // set to ID 1 in DriverStation
	Joystick rightStick; // set to ID 2 in DriverStation
	Joystick operatorStick;

	TalonSRX frontRight;
	TalonSRX frontLeft;
	TalonSRX backRight;
	TalonSRX backLeft;
	TalonSRX middleRight;
	TalonSRX middleLeft;
	
	TalonSRX shooter;
	TalonSRX climber;
	TalonSRX intake;
	
	Servo intakeFeed;
	Servo GearOne;
	Servo GearTwo;
	
	Victor intakeLED;

	AHRS ahrs;

	PowerDistributionPanel pdp;

	DigitalInput GearSensor = new DigitalInput(0);

	Encoder encLeft;
	Encoder encRight;
	
	int countR;
	double rawDistanceR;
	double distanceR;
	double rateR;
	boolean directionR;
	boolean stoppedR;

	int countL;
	double rawDistanceL;
	double distanceL;
	double rateL;
	boolean directionL;
	boolean stoppedL;
	
	double targetDistance;


	// VISION DATA STRUCTURES
	UsbCamera shooterCamera;
	UsbCamera intakeCamera;
	VisionThread shooterVisionThread;
	VisionThread intakeVisionThread;
	
	CvSource shooterOutputStream;
	CvSource intakeOutputStream;
	
	int exposureValue = 10;
	boolean exposureChanged = false;

	// Vision Parameters
	double hueMin = 50;
	double hueMax = 122;
	double satMin = 105;
	double satMax = 255;
	double valMin = 50;
	double valMax = 255;
	
	double F = 0.027;
	double P = 0.05;
	double I = 0.0;
	double D = 1.0;

	
	// Auton structures
	public int AutonStage;
	public enum AutonMode { START, DRIVE_STRAIGHT, TURN, DELAY, SHOOT, RELEASE_GEAR,  MOVE_TO_GEAR, BACK_UP, STOP };

	AutonMode currentAutonMode = AutonMode.START;
	public int currentAutonStage = 0;
	public long startTime; // Used to measure elapsed time
	
	// double autonDistance = 2.4;
	double autonShootTime = 5.0;
	
	double autonD1 = 2.4;
	double autonTurnAngle = 60;
	double autonD2 = 0.6;
	int autonDropGear = 1;

	double shooterRPM = SHOOTER_RPM_PRE_AUTON;
	
	// gear handler height
	double gearHeight = 0.55;

	public Robot() {
		// The code below sets up the Joysticks and talons for the drivetrain.
		//(front or the back of the robot)(Right or Left of the robot)

		// left CAN IDs:
		// right CAN IDs: 4, 5, 6
		// shooter CAN ID: 7
		// intake CAN ID: 8
		// climber CAN ID: 9
		frontLeft   = new TalonSRX(1);
		middleLeft  = new TalonSRX(2);
		backLeft    = new TalonSRX(3);

		frontRight  = new TalonSRX(4);
		middleRight = new TalonSRX(5);
		backRight   = new TalonSRX(6);

		frontRight.setInverted(true);
		backRight.setInverted(true);
		middleRight.setInverted(true);


		leftStick  = new Joystick(0);
		rightStick = new Joystick(1);
		operatorStick = new Joystick(2);

		shooter = new TalonSRX(7);
		intake  = new TalonSRX(8);
		climber = new TalonSRX(9);
		
		intakeFeed = new Servo(0);
		GearOne = new Servo(5);
		GearTwo = new Servo(6);

		shooter.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0, 0);

		shooter.setSensorPhase(true);
		shooter.setInverted(true);

		shooter.configNominalOutputForward(0.0, 0);
		shooter.configNominalOutputReverse(0.0, 0);
		shooter.configPeakOutputForward(0.0, 0);
		shooter.configPeakOutputReverse(-1.0, 0);
		
		shooter.setNeutralMode(NeutralMode.Coast);

		shooter.selectProfileSlot(0, 0);
		shooter.config_kF(0, F, 0);
		shooter.config_kP(0, P, 0);
		shooter.config_kI(0, I, 0);
		shooter.config_kD(0, D, 0);
	//	shooter.SetVelocityMeasurementWindow(512);
		
		// only allow climber to go in one direction
		climber.configNominalOutputForward(0.0, 0);
		climber.configNominalOutputReverse(0.0, 0);
		climber.configPeakOutputForward(0.0, 0);
		climber.configPeakOutputReverse(-1.0, 0);

		pdp = new PowerDistributionPanel(); 

		// camera paths:
		//    top USB: /dev/v4l/by-path/platform-ci_hdrc.0-usb-0:1.1:1.0-video-index0
		// bottom USB: /dev/v4l/by-path/platform-ci_hdrc.0-usb-0:1.2:1.0-video-index0

		intakeLED = new Victor(9);

		shooterCamera = CameraServer.getInstance().startAutomaticCapture("shooter", "/dev/v4l/by-path/platform-ci_hdrc.0-usb-0:1.1:1.0-video-index0");
		intakeCamera = CameraServer.getInstance().startAutomaticCapture("intake", "/dev/v4l/by-path/platform-ci_hdrc.0-usb-0:1.2:1.0-video-index0");
	}

	@Override
	public void robotInit() {

		try {
			/***********************************************************************
			 * navX-MXP:
			 * - Communication via RoboRIO MXP (SPI, I2C, TTL UART) and USB.            
			 * - See http://navx-mxp.kauailabs.com/guidance/selecting-an-interface.
			 * 
			 * navX-Micro:
			 * - Communication via I2C (RoboRIO MXP or Onboard) and USB.
			 * - See http://navx-micro.kauailabs.com/guidance/selecting-an-interface.
			 * 
			 * Multiple navX-model devices on a single robot are supported.
			 ************************************************************************/
			ahrs = new AHRS(SPI.Port.kMXP);
			ahrs.zeroYaw();
		} catch (RuntimeException ex ) {
			DriverStation.reportError("Error instantiating navX MXP:  " + ex.getMessage(), true);
		}

		encLeft = new Encoder(8, 9, false, CounterBase.EncodingType.k4X);

		encLeft.setMaxPeriod(1);
		encLeft.setMinRate(0.1);
		encLeft.setDistancePerPulse(1.0/((4.0 * 25.4 / .001) * Math.PI / 360.0));
		encLeft.setReverseDirection(false);
		encLeft.setSamplesToAverage(7);

		encRight = new Encoder(2, 3, false, CounterBase.EncodingType.k4X);
		
		encRight.setMaxPeriod(1);
		encRight.setMinRate(0.1);
		encRight.setDistancePerPulse(1.0/((4.0 * 25.4 / .001) * Math.PI / 360.0));
		encRight.setReverseDirection(false);
		encRight.setSamplesToAverage(7);

		
		shooterCamera.setResolution(320, 240);
		intakeCamera.setResolution(320, 240);

		shooterOutputStream = CameraServer.getInstance().putVideo("shooterOverlay", 320, 240);
		intakeOutputStream = CameraServer.getInstance().putVideo("intakeOverlay", 320, 240);
		

		shooterVisionThread = new VisionThread(shooterCamera, new LineVisionPipeline(),
				pipeline->{
					pipeline.g = 0;
					pipeline.b = 0;
					pipeline.r = 255;
					pipeline.width = 2;
					shooterOutputStream.putFrame(pipeline.lineOutput());
				});

		intakeVisionThread = new VisionThread(intakeCamera, new LineVisionPipeline(),
				pipeline->{
					pipeline.g = 0;
					pipeline.b = 255;
					pipeline.r = 0;
					pipeline.width = 2;
					intakeOutputStream.putFrame(pipeline.lineOutput());
				});

		/***
		intakeVisionThread = new VisionThread(intakeCamera, new OurVisionPipeline(),
				pipeline->{
					intakeOutputStream.putFrame(pipeline.overlayOutput());

					if (pipeline.foundTarget) {
						targetDistance = pipeline.targetDistance;
					} else {
						targetDistance = -1.0;
					}
					
					if (exposureChanged) {
						intakeCamera.setExposureManual(exposureValue);
						exposureChanged = false;						
					}

					pipeline.hsvThresholdHue[0] = hueMin;
					pipeline.hsvThresholdHue[1] = hueMax;

					pipeline.hsvThresholdSaturation[0] = satMin;
					pipeline.hsvThresholdSaturation[1] = satMax;

					pipeline.hsvThresholdValue[0] = valMin;
					pipeline.hsvThresholdValue[1] = valMax;
				});
		***/
		/***
		shooterVisionThread = new VisionThread(shooterCamera, new ShooterVisionPipeline(),
				pipeline->{
					shooterOutputStream.putFrame(pipeline.overlayOutput());

					if (exposureChanged) {
						shooterCamera.setExposureManual(exposureValue);
						exposureChanged = false;						
					}

					pipeline.hsvThresholdHue[0] = hueMin;
					pipeline.hsvThresholdHue[1] = hueMax;

					pipeline.hsvThresholdSaturation[0] = satMin;
					pipeline.hsvThresholdSaturation[1] = satMax;

					pipeline.hsvThresholdValue[0] = valMin;
					pipeline.hsvThresholdValue[1] = valMax;
				});
		***/
		
		shooterVisionThread.start();
		
		intakeVisionThread.start();

		SmartDashboard.putNumber("Exposure", exposureValue);

		SmartDashboard.putNumber("hueMin", hueMin);
		SmartDashboard.putNumber("hueMax", hueMax);
		SmartDashboard.putNumber("satMin", satMin);
		SmartDashboard.putNumber("satMax", satMax);
		SmartDashboard.putNumber("valMin", valMin);
		SmartDashboard.putNumber("valMax", valMax);

		SmartDashboard.putNumber("F", F);
		SmartDashboard.putNumber("P", P);
		SmartDashboard.putNumber("I", I);
		SmartDashboard.putNumber("D", D);
		
		// Set auton parameters on smart dashboard
		reportShooters();
		// SmartDashboard.putNumber("autonDistance", autonDistance);
		SmartDashboard.putNumber("autonShootTime", autonShootTime);

		SmartDashboard.putNumber("autonD1", autonD1);
		SmartDashboard.putNumber("autonTurnAngle", autonTurnAngle);
		SmartDashboard.putNumber("autonD2", autonD2);
		SmartDashboard.putNumber("autonDropGear", autonDropGear);

		openServo(false);
		gearServo(false);
	}
	
	// TELEOP MODE

	@Override
	public void teleopInit() {
		ahrs.zeroYaw();
		intakeCamera.setExposureAuto();
		shooterCamera.setExposureAuto();
		
		shooterRPM = SHOOTER_RPM_PRE_TELEOP;
		
		openServo(false);
		gearServo(false);
	}


	@Override
	public void teleopPeriodic () {
		// these need to be negated because forward on the stick is negative
		double leftAxis  = -leftStick.getY();
		double rightAxis = -rightStick.getY();

		double scale = getDrivePowerScale();
		adaptiveDrive(leftAxis * scale, rightAxis * scale);
		
		climber.set(ControlMode.PercentOutput, operatorStick.getRawAxis(5)); // Right X of Joystick
		
		intake.set(ControlMode.PercentOutput, operatorStick.getRawAxis(1)); // Left X of Joystick
		
		// shooter enable/disable
		if (operatorStick.getRawAxis(3) > 0.15) {
			shooter.set(ControlMode.Velocity, shooterRPM);
		} else {
			shooter.set(ControlMode.PercentOutput, 0);
		}
		
		// intake servo enable/disable
		//openServo(operatorStick.getRawAxis(2) > 0.15);
		openServo(operatorStick.getRawButton(4)); // Y button
		
		// gear mech servo enable/disable
		gearServo(operatorStick.getRawButton(4)); // Y button
		
		intakeLED.set(0.05);
		
		// set shooter rpm
		setShooterRPM();

		report();

		double newF = SmartDashboard.getNumber("F", F);
		if(!compareDoubles(newF, F)) {
			F = newF;
			shooter.config_kF(0, F, 0);
		}
		double newP = SmartDashboard.getNumber("P", P);
		if(newP != P) {
			P = newP;
			shooter.config_kP(0, P, 0);
		}
		double newI = SmartDashboard.getNumber("I", I);
		if(!compareDoubles(newI, I)) {
			I = newI;
			shooter.config_kI(0, I, 0);
		}
		double newD = SmartDashboard.getNumber("D", D);
		if(!compareDoubles(newD, D)) {
			D = newD;
			shooter.config_kD(0, D, 0);
		}
		
		int exposureValueNew = (int) SmartDashboard.getNumber("Exposure", exposureValue);
		if(exposureValueNew != exposureValue) {
			exposureValue = exposureValueNew;
			exposureChanged = true;
		}

		hueMin = SmartDashboard.getNumber("hueMin", hueMin);
		hueMax = SmartDashboard.getNumber("hueMax", hueMax);
		satMin = SmartDashboard.getNumber("satMin", satMin);
		satMax = SmartDashboard.getNumber("satMax", satMax);
		valMin = SmartDashboard.getNumber("valMin", valMin);
		valMax = SmartDashboard.getNumber("valMax", valMax);
		
	}
	
	public void openServo(boolean open) {
		if (open) {
			intakeFeed.set(0.0);
		} else {
			intakeFeed.set(0.5);
		}
	}
	
	public void gearServo(boolean open) {
		if(open) {
			GearOne.set(0.45);
			GearTwo.set(0.85);
		} else {
			GearOne.set(0.45 + gearHeight); //1.0 - heightAdjust);
			GearTwo.set(0.85 - gearHeight);
		}
	}
	
	public double getDrivePowerScale() {
		double scale = 0.65;

		if ( leftStick.getTrigger() || rightStick.getTrigger() ) {
			scale = 0.85;
		}
		
		if (leftStick.getTrigger() && rightStick.getTrigger()) {
			scale = 1;
		}
		
		return scale;
	}


	// AUTONOMOUS MODE
	
	@Override
	public void autonomousInit() {
		currentAutonStage = 1;
		currentAutonMode = AutonMode.START;
		
		// Read auton parameters from smart dashboard
		// Read shooter RPM
		shooterRPM = SmartDashboard.getNumber("shooterRPM", shooterRPM);
		
		// Read auton distance +ve or -ve if 0 not movement
		// autonDistance = SmartDashboard.getNumber("autonDistance", autonDistance);
		
		// Set shoot time in seconds if <= 0 no shot
		autonShootTime = SmartDashboard.getNumber("autonShootTime", autonShootTime);
		
		autonD1 = SmartDashboard.getNumber("autonD1", autonD1);
		autonTurnAngle = SmartDashboard.getNumber("autonTurnAngle", autonTurnAngle);
		autonD2 = SmartDashboard.getNumber("autonD2", autonD2);
		autonDropGear = (int) SmartDashboard.getNumber("autonDropGear", autonDropGear);
		
		openServo(false);
		gearServo(false);
	}
	

	@Override
	public void autonomousPeriodic() {
		switch (currentAutonStage) {
		case 1: // Auton Shooting Mode
			AutonShoot (shooterRPM, autonShootTime);
			//AutonSideGear (0.35, 0.35, autonD1, autonTurnAngle, autonD2, 5.0);
			break;
		case 2: // Auton Drive Straight Mode
			AutonDriveBack (0.4, autonD1);
			//AutonDriveStraight (0.4, autonD1);

			// Distance Parameters need to be recomputed
			// AutonSideGear (0.35, 0.35, autonD1, autonTurnAngle, autonD2, 5.0);
			break;
		}
		
		// Advance the stage after each action
		if (currentAutonMode == AutonMode.STOP) {
			++currentAutonStage;
			currentAutonMode = AutonMode.START;
		}
		
		SmartDashboard.putNumber ("currentAutonStage", currentAutonStage);
		SmartDashboard.putString ("currentAutonMode", currentAutonMode.toString());
		
		report();
	}
	
	// Place gear on side peg, d1 first distance, d2 second distance, timeout before backup
	public void AutonSideGear (double forward_power, double turn_power, double d1, double turn_angle, double d2, double timeout) {
		// START
		// Go straight for d1
		// Turn by angle, +60 for left -60 for right
		// Go straight for d2 or until timeout
		// Drop gear
		// Wait for one second
		// Backup 1 meter
		// STOP
		
		double temp;
		
		switch (currentAutonMode) {
		case START:
			resetDistanceAndYaw();
			currentAutonMode = AutonMode.DRIVE_STRAIGHT;
			break;
		case DRIVE_STRAIGHT:
			driveStraight(Math.signum(d1) * Math.abs(forward_power));
			if ((d1 == 0.0) || (Math.abs(getEncoderDistance()) > Math.abs(d1))) {
				driveStraight(0.0);
				resetDistanceAndYaw();
				currentAutonMode = AutonMode.TURN;
			}
			break;
		case TURN:
			temp = Math.signum(turn_angle) * turn_power;
			setDriveMotors(temp, -temp);
			if ((turn_angle == 0.0) || (Math.abs(ahrs.getAngle()) > Math.abs(turn_angle))) {
				setDriveMotors(0.0, 0.0);
				resetDistanceAndYaw();
				resetTimer();
				currentAutonMode = AutonMode.MOVE_TO_GEAR;
			}
			break;
		case MOVE_TO_GEAR:
			driveStraight(Math.signum(d2) * Math.abs(forward_power));
			// Note that we check the timeout to handle situations where you drive into the airship
			if  ((d2 == 0.0) || (Math.abs(getEncoderDistance()) > Math.abs(d2)) || (elapsedTime() > timeout) ) {
				driveStraight(0.0);
				resetDistanceAndYaw();
				resetTimer();
				currentAutonMode = AutonMode.RELEASE_GEAR;
			}
			break;
		case RELEASE_GEAR:
			if (autonDropGear > 0) {
				gearServo(true);
			} else {
				currentAutonMode = AutonMode.STOP;
			}
			
			if (elapsedTime() > 0.5) {
				resetDistanceAndYaw();
				resetTimer();
				currentAutonMode = AutonMode.BACK_UP;
			}
			break;
			/***
		case BACK_UP:
			driveStraight(-1.0 * Math.abs(forward_power));
			if (Math.abs(getEncoderDistance()) > 1.0) {
				driveStraight(0.0);
				resetDistanceAndYaw();
				currentAutonMode = AutonMode.STOP;
			}
			break;
			 ***/
		case BACK_UP:
			driveStraight(-1.0 * Math.abs(forward_power));
			shooter.set(ControlMode.Velocity, shooterRPM); // Warm up the shooter

			if  ((d2 == 0.0) || (Math.abs(getEncoderDistance()) > Math.abs(d2)) || (elapsedTime() > timeout) ) {
				driveStraight(0.0);
				resetDistanceAndYaw();
				resetTimer();
				openServo(true);
				currentAutonMode = AutonMode.SHOOT;
			}
			break;
		case SHOOT:
			if (elapsedTime() >= autonShootTime) {
				currentAutonMode = AutonMode.STOP;
				shooter.set(ControlMode.PercentOutput, 0);
				openServo(false);
			}
			break;
		default:
			break;
		}
	}

	public double getEncoderDistance () {
		// Factor of 0.9 to account for travel on carpet
		return (0.9 * encRight.getDistance());
	}


	// Auto shooter to be invoked by pressing and holding a button, first squeeze resets state to start
	public void AutoShooter () {
		// Check if the shooter target is in view - if not state = STOP

		// Turn to shooter target and set RPM based on height of target in image - need function to compute this mapping

		// Wait for system to spin up

		// OpenServo and let fly
	}

	public void AutonShoot (double RPM, double shootTime) {
		
		if (shootTime <= 0) {
			currentAutonMode = AutonMode.STOP;
			return;
		}
		
		switch (currentAutonMode) {
		case START:
			resetTimer();
			currentAutonMode = AutonMode.DELAY;
			shooter.set(ControlMode.Velocity, RPM);
			break;
		case DELAY:
			// Give the shooter a second to spin up
			if (elapsedTime() >= 1.0) {		
				shooter.configNominalOutputForward(0.0, 0);
				shooter.configNominalOutputReverse(0.0, 0);
				shooter.configPeakOutputForward(0.0, 0);
				shooter.configPeakOutputReverse(-1.0, 0);
				resetTimer();
				currentAutonMode = AutonMode.SHOOT;
				openServo(true);
			}
			break;
		case SHOOT:
			if (elapsedTime() >= shootTime) {
				currentAutonMode = AutonMode.STOP;
				shooter.set(ControlMode.PercentOutput, 0);
				openServo(false);
			}
			break;
		default:
			break;
		}
	}

	public void AutonDriveStraight (double power, double distance) {
		
		if (distance == 0) {
			currentAutonMode = AutonMode.STOP;
			return;
		}
		
		switch (currentAutonMode) {
		case START: 
			resetDistanceAndYaw(); 
			currentAutonMode = AutonMode.DRIVE_STRAIGHT;
			break;
		case DRIVE_STRAIGHT :
			driveStraight(Math.signum(distance) * Math.abs(power));
			if (Math.abs(getEncoderDistance()) > Math.abs(distance)) {
				driveStraight(0.0);
				currentAutonMode = AutonMode.STOP;
			}
			break;
		default:
			break;
		}
	}
public void AutonDriveBack (double power, double distance) {
		
		if (distance == 0) {
			currentAutonMode = AutonMode.STOP;
			return;
		}
		
		switch (currentAutonMode) {
		case START: 
			resetDistanceAndYaw(); 
			currentAutonMode = AutonMode.DRIVE_STRAIGHT;
			break;
		case DRIVE_STRAIGHT :
			driveStraight(-(Math.signum(distance) * Math.abs(power)));
			if (Math.abs(getEncoderDistance()) > Math.abs(distance)) {
				driveStraight(0.0);
				currentAutonMode = AutonMode.STOP;
			}
			break;
		default:
			break;
		}
	}
	
	public void resetTimer () {
		startTime = System.nanoTime();
	}
	
	// Return elapsed time since last reset in seconds
	public double elapsedTime () {
		return ( (System.nanoTime() - startTime) * 1.0e-9 );
	}

	
	void resetDistanceAndYaw () {
		ahrs.zeroYaw();
		encLeft.reset();
		encRight.reset();
	}

	// UTILITY METHODS

	public void setDriveMotors(double l, double r) {
		frontRight.set(ControlMode.PercentOutput, r);
		backRight.set(ControlMode.PercentOutput, r);
		middleRight.set(ControlMode.PercentOutput, r);

		frontLeft.set(ControlMode.PercentOutput, l);
		backLeft.set(ControlMode.PercentOutput, l);
		middleLeft.set(ControlMode.PercentOutput, l);
	}
	
	int rpmButton = -1;
	int shooterRPMIter = 0;
	public void setShooterRPM() {
		int button;
		// set shooter rpm
		button = operatorStick.getPOV();
		if ( ( (rpmButton != 0) && (button == 0) ) || ((rpmButton == 0) && (shooterRPMIter%10 == 0)) ) {
			if(shooterRPM < 6000) shooterRPM += 100;
			shooterRPMIter = 0;
		}
		
		if ( ( (rpmButton != 180) && (button == 180) ) || ((rpmButton == 180) && (shooterRPMIter%10 == 0)) ) {
			if (shooterRPM > 0) shooterRPM -= 100;
			shooterRPMIter = 0;
		}
		rpmButton = button;
		++shooterRPMIter;
	}
	
	public void adaptiveDrive(double l, double r) {

		// alpha is a parameter between 0 and 1
		final double alpha = 0.5;
		double c = 0.5 * (l+r);
		double d = 0.5 * (l-r);
		double scale = (1 - (alpha * c * c));
		d *= scale;

		// GYRO CORRECTION -- high if d is close to zero, low otherwise
		double gRate = ahrs.getRate();
		final double CORR_COEFF = 0.5;
		double corr = 0.0;
		if (Math.abs(d) < 0.05)
			corr = (gRate * Math.abs(c) * CORR_COEFF * (1-Math.abs(d)));
		d -= corr;

		double l_out = c + d;
		double r_out = c - d;

		setDriveMotors(l_out, r_out);
	}
	
	// driveStraight implements a simple proportional controller designed to regulate the yaw angle to zero
	public void driveStraight (double c) {
		double d = -0.01 * ahrs.getAngle();
		setDriveMotors (c+d, c-d);
	}
	
 	public void reportEncoders() {
 		//SmartDashboard.putNumber("Left Distance", distanceL);
 		SmartDashboard.putNumber("Right Distance", distanceR);
 		/*
		SmartDashboard.putNumber("Left Count", countL);
		SmartDashboard.putNumber("Left Raw Distance", rawDistanceL);
		SmartDashboard.putNumber("Left Distance", distanceL);
		SmartDashboard.putNumber("Left Rate", rateL);
		SmartDashboard.putBoolean("Left Direction", directionL);
		SmartDashboard.putBoolean("Left Stopped", stoppedL);

		SmartDashboard.putNumber("Right Count", countR);
		SmartDashboard.putNumber("Right Raw Distance", rawDistanceR);
		SmartDashboard.putNumber("Right Rate", rateR);
		SmartDashboard.putBoolean("Right Direction", directionR);
		SmartDashboard.putBoolean("Right Stopped", stoppedR);
		*/
	}

	public void reportShooters() {
		SmartDashboard.putNumber("shooterRPM", shooterRPM);
		SmartDashboard.putNumber("shooterSpeed", shooter.getSelectedSensorVelocity(0));

		/*
		 * SmartDashboard.putNumber(" V", shooter.getOutputVoltage());
		 * SmartDashboard.putNumber("SP", shooter.getSetpoint());
		 * SmartDashboard.putNumber(" E", shooter.getError());
		 */
	}

	public void reportAhrs() {
		/* Display 6-axis Processed Angle Data                                      */
		SmartDashboard.putBoolean(  "IMU_Connected",        ahrs.isConnected());
		SmartDashboard.putBoolean(  "IMU_IsCalibrating",    ahrs.isCalibrating());
		SmartDashboard.putNumber(   "IMU_Yaw",              ahrs.getYaw());
		SmartDashboard.putNumber(   "IMU_YawRate",          ahrs.getRate());
		SmartDashboard.putNumber(   "IMU_Pitch",            ahrs.getPitch());
		SmartDashboard.putNumber(   "IMU_Roll",             ahrs.getRoll());

		/* Display 9-axis Heading (requires magnetometer calibration to be useful)  */
		SmartDashboard.putNumber(   "IMU_FusedHeading",     ahrs.getFusedHeading());

		SmartDashboard.putNumber(   "IMU_TotalYaw",         ahrs.getAngle());

		/* Omnimount Yaw Axis Information                                           */
		/* For more info, see http://navx-mxp.kauailabs.com/installation/omnimount  */
		AHRS.BoardYawAxis yaw_axis = ahrs.getBoardYawAxis();
		SmartDashboard.putString(   "YawAxisDirection",     yaw_axis.up ? "Up" : "Down" );
		SmartDashboard.putNumber(   "YawAxis",              yaw_axis.board_axis.getValue() );
	}

	public void reportPower() {
		double current = pdp.getTotalCurrent();
		double power = pdp.getTotalPower();
		
		SmartDashboard.putNumber("Current", current);
		SmartDashboard.putNumber("Power", power);
	}

	public void reportAndUpdateGearHeight() {
		double gh = SmartDashboard.getNumber("GearHeight", gearHeight);
		if(!compareDoubles(gh, gearHeight)) {
			gearHeight = gh;
		}
		SmartDashboard.putNumber("GearHeight", gearHeight);
	}
	public void report() {
		reportShooters();
		// reportAhrs();
		updateEncoders();
		reportEncoders();
		reportPower();
		reportAndUpdateGearHeight();
		SmartDashboard.putNumber("Target Distance", targetDistance); 
	}

	public void updateEncoders() {
		countR = encRight.get();
		rawDistanceR = encRight.getRaw();
		distanceR = encRight.getDistance();
		rateR = encRight.getRate();
		directionR = encRight.getDirection();
		stoppedR = encRight.getStopped();

		countL = encLeft.get();
		rawDistanceL = encLeft.getRaw();
		distanceL = encLeft.getDistance();
		rateL = encLeft.getRate();
		directionL = encLeft.getDirection();
		stoppedL = encLeft.getStopped();
	}
	
	public static boolean compareDoubles(double expected, double actual)
	{
		return Math.abs(expected-actual) < 5 * Math.ulp(expected);
	}
	
	@Override
	public void disabledPeriodic() {
		setShooterRPM();
		report();
	}

}
